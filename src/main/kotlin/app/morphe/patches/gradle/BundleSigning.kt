/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches-gradle-plugin
 */

package app.morphe.patches.gradle

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.gradle.api.GradleException
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Signs patch bundles the way Morphe Patcher verifies them, see `PatchBundleSignature` there.
 *
 * The private key is a Base64 raw Ed25519 key, read from the `MORPHE_SIGNING_KEY` environment
 * variable or else the [PRIVATE_KEY_FILE] of the root project. The public key is committed as
 * [PUBLIC_KEY_FILE] so patch sources can publish it.
 */
internal object BundleSigning {
    const val PRIVATE_KEY_FILE = "morphe-signing.key"
    const val PUBLIC_KEY_FILE = "morphe-signing.pub"

    private const val PRIVATE_KEY_ENVIRONMENT_VARIABLE = "MORPHE_SIGNING_KEY"
    private const val MAGIC = "MorpheSig1"
    private const val EOCD_SIGNATURE = 0x06054b50
    private const val EOCD_SIZE = 22

    private val encoder = Base64.getEncoder()

    /**
     * Writes a new key pair to [directory], unless one is there already.
     */
    fun generateKey(directory: File) {
        val privateKeyFile = directory.resolve(PRIVATE_KEY_FILE)
        val publicKeyFile = directory.resolve(PUBLIC_KEY_FILE)
        if (privateKeyFile.exists() || publicKeyFile.exists()) {
            throw GradleException("$PRIVATE_KEY_FILE or $PUBLIC_KEY_FILE already exists, delete both to create a new key")
        }

        val privateKey = Ed25519PrivateKeyParameters(SecureRandom())
        privateKeyFile.writeText(encoder.encodeToString(privateKey.encoded) + "\n")
        publicKeyFile.writeText(encoder.encodeToString(privateKey.generatePublicKey().encoded) + "\n")
    }

    /**
     * Signs [bundle] if a private key is set.
     *
     * @return False if no private key is set and [bundle] stays unsigned.
     */
    fun sign(bundle: File, rootDirectory: File): Boolean {
        val privateKey = readPrivateKey(rootDirectory) ?: return false
        val publicKey = encoder.encodeToString(privateKey.generatePublicKey().encoded)

        // A key that does not match the published one signs bundles no patch source accepts
        rootDirectory.resolve(PUBLIC_KEY_FILE).takeIf(File::exists)?.readText()?.trim()?.let { published ->
            if (published != publicKey) {
                throw GradleException("The signing key does not match $PUBLIC_KEY_FILE")
            }
        }

        RandomAccessFile(bundle, "rw").use { archive ->
            // The archive was just built, so it ends with an end of central directory record without a comment
            val eocd = ByteArray(EOCD_SIZE).also { archive.seek(archive.length() - EOCD_SIZE); archive.readFully(it) }
            fun int(at: Int, bytes: Int) = (0 until bytes).fold(0) { value, i ->
                value or ((eocd[at + i].toInt() and 0xFF) shl (8 * i))
            }
            if (int(0, 4) != EOCD_SIGNATURE || int(EOCD_SIZE - 2, 2) != 0) {
                throw GradleException("$bundle is already signed or not a zip archive")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            archive.seek(0)
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = archive.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }

            val message = MAGIC.toByteArray(Charsets.US_ASCII) + digest.digest()
            val signature = Ed25519Signer().run {
                init(true, privateKey)
                update(message, 0, message.size)
                generateSignature()
            }

            val comment = "$MAGIC $publicKey ${encoder.encodeToString(signature)}".toByteArray(Charsets.US_ASCII)
            archive.seek(archive.length() - 2)
            archive.write(comment.size and 0xFF)
            archive.write(comment.size shr 8)
            archive.write(comment)
        }

        return true
    }

    private fun readPrivateKey(rootDirectory: File): Ed25519PrivateKeyParameters? {
        val key = System.getenv(PRIVATE_KEY_ENVIRONMENT_VARIABLE)?.takeIf(String::isNotBlank)
            ?: rootDirectory.resolve(PRIVATE_KEY_FILE).takeIf(File::exists)?.readText()
            ?: return null

        return try {
            Ed25519PrivateKeyParameters(Base64.getDecoder().decode(key.trim()))
        } catch (_: IllegalArgumentException) {
            throw GradleException("The signing key is not a Base64 Ed25519 key")
        }
    }
}
