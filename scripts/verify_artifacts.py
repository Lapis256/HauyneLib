"""Inspect real publication artifacts; never contacts external services."""
import io
import json
import os
import sys
import tomllib
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

props = dict(
    line.split('=', 1) for line in Path('gradle.properties').read_text().splitlines() if '=' in line
)

for module in ['core', 'ksp', 'memory', 'nightconfig']:
    classes = list(Path(f'hauyne-config/{module}/build/classes/kotlin/main').rglob('*.class'))
    assert classes, module
    assert all(int.from_bytes(file.read_bytes()[6:8], 'big') == 61 for file in classes), module
    if module != 'core':
        package = f'dev/lapis256/hauyne/config/{module}/'
        assert all(package in file.as_posix() for file in classes), module

example = Path('examples/jvm/build/classes/kotlin/main/dev/lapis256/hauyne/example/Example.class')
assert int.from_bytes(example.read_bytes()[6:8], 'big') == 65
print('Shared JVM libraries: Java 17; JVM example: Java 21 verified')

repo = Path.home() / '.m2/repository'

# Respect Maven's standard local repository override when used in CI/tests.
repo = Path(os.environ.get('MAVEN_LOCAL_REPO', str(repo)))

settings = Path.home() / '.m2/settings.xml'
if 'MAVEN_LOCAL_REPO' not in os.environ and settings.exists():
    configured = ET.parse(settings).findtext('{*}localRepository')
    if configured:
        repo = Path(configured)

for artifact in ['hauyne-config-core', 'hauyne-config-ksp', 'hauyne-config-nightconfig']:
    base = repo / Path(props['mavenGroup'].replace('.', '/')) / artifact / props['modVersion']
    pom = ET.parse(base / f"{artifact}-{props['modVersion']}.pom")
    assert pom.findtext('{*}groupId') == 'dev.lapis256'
    assert pom.findtext('{*}artifactId') == artifact

for mc in sys.argv[1:] or ['26.1.2', '1.21.1']:
    loader = 'forge' if mc == '1.20.1' else 'neoforge'
    series = {'1.20.1': '20.1', '1.21.1': '21.1', '26.1.2': '26.1'}[mc]
    version = f"{series}-{props['modVersion']}"
    jar = Path(f'hauyne-lib/{loader}/versions/{mc}/build/libs/hauyne-lib-{loader}-{version}.jar')
    with zipfile.ZipFile(jar) as z:
        metadata = 'META-INF/mods.toml' if loader == 'forge' else 'META-INF/neoforge.mods.toml'
        assert z.read(f"LICENSE_{props['modId']}") == Path('LICENSE').read_bytes()
        mod = tomllib.loads(z.read(metadata).decode())
        assert mod['mods'][0]['modId'] == props['modId']
        assert mod['mods'][0]['version'] == version

        deps = mod['dependencies'][props['modId']]
        assert [dep['modId'] for dep in deps] == ['minecraft', loader]
        assert deps[0]['versionRange'] == f'[{mc}]'
        assert all(
            dep.get('mandatory') is True if loader == 'forge' else dep.get('type') == 'required'
            for dep in deps
        )

        jij = json.loads(z.read('META-INF/jarjar/metadata.json'))
        nested = jij['jars']
        names = [
            next(
                (
                    name
                    for name in ['hauyne-config-core', 'hauyne-config-minecraft', f'hauyne-config-{loader}']
                    if name + '-' in item['path']
                ),
                item['identifier']['artifact'],
            )
            for item in nested
        ]

        for required in ['hauyne-config-core', 'hauyne-config-minecraft', f'hauyne-config-{loader}']:
            assert names.count(required) == 1, (mc, required, names)
        assert not any('ksp' in n for n in names)
        assert not any(
            item['identifier']['group'].startswith('org.jetbrains.kotlin') or 'kotlin-stdlib' in item['path']
            for item in nested
        )

        mc_props = dict(
            line.split('=', 1)
            for line in Path(f'versions/{mc}/gradle.properties').read_text().splitlines()
            if '=' in line
        )
        dependencies = {entry['modId']: entry for entry in mod['dependencies'][props['modId']]}
        assert set(dependencies) == {'minecraft', loader}
        assert dependencies['minecraft']['versionRange'] == f'[{mc}]'
        assert dependencies[loader]['versionRange'] == mc_props['loaderVersionRange']
        assert mod['loaderVersion'] == mc_props['javaFmlLoaderVersionRange']
        assert 'features' not in mod
        for entry in dependencies.values():
            if loader == 'forge':
                assert entry['mandatory'] is True and 'type' not in entry
            else:
                assert entry['type'] == 'required' and 'mandatory' not in entry

        major = int(mc_props['javaVersion']) + 44
        assert int.from_bytes(z.read(f'dev/lapis256/hauyne/lib/{loader}/HauyneLib.class')[6:8], 'big') == major
        assert 'dev/lapis256/hauyne/HauyneLib.class' not in z.namelist()
        assert f'dev/lapis256/hauyne/lib/{loader}/HauyneConstants.class' in z.namelist()

        for item in nested:
            with zipfile.ZipFile(io.BytesIO(z.read(item['path']))) as child:
                assert child.read(f"LICENSE_{props['modId']}") == Path('LICENSE').read_bytes()
                if 'hauyne-config-core-' in item['path']:
                    assert int.from_bytes(child.read('dev/lapis256/hauyne/config/Backend.class')[6:8], 'big') == 61
                elif 'hauyne-config-' in item['path']:
                    classes = [name for name in child.namelist() if name.endswith('.class')]
                    assert classes, item['path']
                    assert all(int.from_bytes(child.read(name)[6:8], 'big') == major for name in classes), item['path']

    for artifact in ['hauyne-config-minecraft', f'hauyne-config-{loader}', f'hauyne-lib-{loader}']:
        base = repo / Path(props['mavenGroup'].replace('.', '/')) / artifact / version
        pom = ET.parse(base / f'{artifact}-{version}.pom')
        assert pom.findtext('{*}groupId') == props['mavenGroup']
        assert pom.findtext('{*}artifactId') == artifact
        module = json.loads((base / f'{artifact}-{version}.module').read_text())
        for variant in module['variants']:
            for dep in variant.get('dependencies', []):
                if (
                    dep['module'].startswith('hauyne-config-')
                    and dep['module'] not in ['hauyne-config-core', 'hauyne-config-ksp', 'hauyne-config-nightconfig']
                ):
                    assert dep['version']['requires'] == version, dep
        assert not any('ksp' in d.findtext('{*}artifactId', '') for d in pom.findall('.//{*}dependency'))

    print(f'{mc}: JAR, Jar-in-Jar, Java target, POM and metadata verified')

ksp = repo / Path(props['mavenGroup'].replace('.', '/')) / 'hauyne-config-ksp' / props['modVersion']
with zipfile.ZipFile(ksp / f"hauyne-config-ksp-{props['modVersion']}.jar") as z:
    service = 'META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider'
    provider = z.read(service).decode().strip()
    assert provider == 'dev.lapis256.hauyne.config.ksp.ConfigProcessorProvider'
    assert provider.replace('.', '/') + '.class' in z.namelist()
