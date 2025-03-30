package cz.seznam.youtrack

import java.io.File
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import jetbrains.exodus.bindings.IntegerBinding
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.PersistentEntityStoreImpl
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.entitystore.PersistentStoreTransaction
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.Environments
import kotlin.system.exitProcess

fun usage() {
    println("Looks for blob files in youtrack DB, that are not linked from the database.")
    println("All such files can be optionally truncated to 0 bytes, to save disk space.")
    println("")
    println("Usage:")
    println("  java -classpath youtrack-db-cleaner-all.jar cz.seznam.youtrack.MainKt [<args>...] <path>")
    println("")
    println("Arguments:")
    println("  -h, --help         Print this help.")
    println("  -v, --verbose      List all the analyzed files and some more information.")
    println("  -d, --delete       Truncate the blob files that should not be present.")
    println("                     Without this option, files are only listed")
    println("                     and no destructive action is taken.")
    println("  <path>             Path to the database, e.g. '/opt/youtrack/data/youtrack'.")
    println("")
    println("IMPORTANT NOTES:")
    println("  1. This is not an official tool, you are using it at your own risk.")
    println("  2. Make a backup of your YouTrack before running with '--delete'.")
    println("  3. YouTrack instance must be stopped before running this.")
    exitProcess(0)
}

fun log(msg: String, verbose:Boolean) {
    if (verbose) {
        println(msg)
    }
}

fun listBlobsInDB(path: String, verbose: Boolean): Set<String> {
    log("Opening DB in $path...", verbose)
    val env: Environment = Environments.newInstance(path)
    val store: PersistentEntityStoreImpl = PersistentEntityStores.newInstance(env, "teamsysstore")
    val blobs: MutableSet<String> = mutableSetOf()

    store.computeInReadonlyTransaction { txn ->
        log("Listing entity types...", verbose)
        val t: PersistentStoreTransaction = txn as PersistentStoreTransaction
        val typeIds = arrayListOf<Int>()
        t.store.entityTypesTable.getSecondIndexCursor(txn.environmentTransaction).use { entityTypesCursor ->
            while (entityTypesCursor.next) {
                typeIds.add(IntegerBinding.compressedEntryToInt(entityTypesCursor.key))
            }
        }

        typeIds.forEach { typeId ->
            val typeName: String = t.store.getEntityType(txn, typeId)
            log("Processing all $typeName...", verbose)
            t.getAll(typeName).forEach { entity: Entity ->
                t.store.getBlobs(t, entity).forEach { blob ->
                    val blobName: String = blob.first
                    val blobHandle: Long = blob.second
                    if (blobHandle < Long.MAX_VALUE - 2) {
                        val loc = t.store.blobVault.getBlobLocation(blobHandle)
                        log("  Found $loc ($typeName.$blobName/$blobHandle)", verbose)
                        blobs.add(loc.path)
                    }
                }
            }
        }
    }

    store.close()
    env.close()

    println("Found ${blobs.size} blobs in DB")
    return blobs
}

fun listFilesOnDisk(path: String, verbose: Boolean): Set<String> {
    log("Listing blob files in $path/blobs...", verbose)
    val matcher = FileSystems.getDefault().getPathMatcher("glob:*.blob")
    val files: Set<String> = Files.walk(Paths.get("$path/blobs"))
        .filter { it?.let { p -> matcher.matches(p.fileName) } ?: false }
        .map { it.toString() }
        .toList()
        .toSet()
    println("Found ${files.size} files on disk")
    return files
}

fun main(args: Array<String>)  {
    lateinit var path: String
    var verbose: Boolean = false
    var delete: Boolean = false
    args.forEach { arg ->
        when (arg) {
            "-h", "-u", "--help", "--usage" -> usage()
            "-v", "--verbose" -> verbose = true
            "-d", "--delete" -> delete = true
            else -> path = arg
        }
    }

    val blobs: Set<String> = listBlobsInDB(path, verbose)
    val files = listFilesOnDisk(path, verbose)

    val canBeDeleted: Set<String> = files - blobs
    log("Blobs that are not linked in DB:", verbose)
    val totalSize: Long = canBeDeleted.map { path ->
        Files.size(Paths.get(path)).also { log("  $path ($it bytes)", verbose) }
    }.sum()

    if (delete) {
        canBeDeleted.forEach {
            log("Deleting $it", verbose)
            File(it).writeText("")
        }
        println("Deleted ${canBeDeleted.size} files ($totalSize bytes)")
    } else {
        println("${canBeDeleted.size} files could be deleted ($totalSize bytes)")
    }
}
