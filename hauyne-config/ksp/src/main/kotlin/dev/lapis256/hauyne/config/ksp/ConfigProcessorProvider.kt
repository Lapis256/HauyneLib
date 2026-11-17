package dev.lapis256.hauyne.config.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

class ConfigProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = object : SymbolProcessor {
        private var generated = false

        override fun process(resolver: Resolver): List<KSAnnotated> {
            if (!generated) {
                val files = resolver.getSymbolsWithAnnotation("dev.lapis256.hauyne.config.Config").mapNotNull {
                    (it as? com.google.devtools.ksp.symbol.KSDeclaration)?.containingFile
                }.toList()
                if (files.isNotEmpty()) {
                    environment.codeGenerator.createNewFile(
                        Dependencies(true, *files.toTypedArray()),
                        "dev.lapis256.hauyne.config.generated",
                        "ConfigMarker",
                    ).writer().use {
                        it.write(
                            """
                            package dev.lapis256.hauyne.config.generated

                            object ConfigMarker {
                                const val generated = true
                            }

                            """.trimIndent(),
                        )
                    }

                    generated = true
                }
            }

            return emptyList()
        }
    }
}
