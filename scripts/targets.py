"""Read the shared Minecraft target declaration for CI and artifact checks."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def load_targets(path=ROOT / 'versions/targets.properties'):
    properties = dict(
        line.split('=', 1)
        for line in path.read_text(encoding='utf-8').splitlines()
        if line and not line.startswith('#')
    )
    default = properties.pop('default')
    targets = {}
    for series, value in properties.items():
        minecraft, loader, release = value.split(',')
        if loader not in ('forge', 'neoforge') or release not in ('true', 'false'):
            raise ValueError(f'Invalid target: {series}={value}')
        if len(series.split('.')) != 2 or not all(part.isdecimal() for part in series.split('.')):
            raise ValueError(f'Invalid series: {series}')
        if len(minecraft.split('.')) < 2 or not all(part.isdecimal() for part in minecraft.split('.')):
            raise ValueError(f'Invalid Minecraft version: {minecraft}')
        targets[series] = {'minecraft': minecraft, 'loader': loader, 'release': release == 'true'}
    if default not in targets or not any(target['release'] for target in targets.values()):
        raise ValueError('A valid default and at least one release target are required')
    return targets


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--release', action='store_true')
    parser.add_argument('--matrix', action='store_true')
    args = parser.parse_args()
    selected = {series: target for series, target in load_targets().items() if not args.release or target['release']}
    if args.matrix:
        print(json.dumps({'include': [{'series': series, **target} for series, target in selected.items()]}))
    else:
        print('\n'.join(selected))
