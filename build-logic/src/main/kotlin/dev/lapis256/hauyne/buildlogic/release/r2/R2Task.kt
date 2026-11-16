package dev.lapis256.hauyne.buildlogic.release.r2

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI
import java.security.MessageDigest

abstract class R2Task : DefaultTask() {
    @get:Internal
    abstract val stagingDirectory: DirectoryProperty

    @get:Internal
    abstract val bucket: Property<String>

    @get:Internal
    abstract val endpoint: Property<String>

    @get:Internal
    abstract val accessKey: Property<String>

    @get:Internal
    abstract val secretKey: Property<String>

    protected open fun createS3Client(): S3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint.get()).also { require(it.scheme == "https") { "R2 endpoint must use HTTPS" } })
        .region(Region.of("auto"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.get(), secretKey.get())))
        .forcePathStyle(true)
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
        .build()
}

internal val metadataSuffixes = listOf("", ".md5", ".sha1", ".sha256", ".sha512")

internal fun metadataBody(key: String): String? = metadataSuffixes
    .firstOrNull { key.endsWith("/maven-metadata.xml$it") }
    ?.let { key.removeSuffix(it) }

internal fun requireMavenPath(path: String) {
    require(
        path.isNotBlank() && '\\' !in path && ':' !in path &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." },
    ) { "Invalid Maven path: $path" }
}

internal fun <T> missingOn404(action: () -> T): T? = try {
    action()
} catch (error: S3Exception) {
    if (error.statusCode() != 404) throw error
    null
}

internal fun remoteSha256(client: S3Client, bucket: String, key: String): String? = missingOn404 {
    client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
