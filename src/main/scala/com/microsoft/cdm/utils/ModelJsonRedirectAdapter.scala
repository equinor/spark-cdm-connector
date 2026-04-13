package com.microsoft.cdm.utils

import com.microsoft.commondatamodel.objectmodel.storage.{AdlsAdapter, StorageAdapterBase}
import java.util.concurrent.CompletableFuture
import java.time.OffsetDateTime
import java.util.{List => JList}
import scala.collection.JavaConverters._

/**
 * Wraps an AdlsAdapter to redirect reads of "model.json" to a custom filename.
 * This is needed because the CDM SDK determines the file format by checking if
 * the filename ends with "model.json". When the actual file has a different name,
 * we tell the CDM SDK it's "model.json" and redirect the adapter reads to the real file.
 */
class ModelJsonRedirectAdapter(val delegate: AdlsAdapter, val customModelJsonName: String) extends StorageAdapterBase {

  private val STANDARD_MODEL_JSON = "model.json"

  /** Redirect corpus paths: CDM SDK asks for paths ending in "model.json", we swap to customModelJsonName */
  private def toStoragePath(corpusPath: String): String = {
    if (corpusPath != null && (corpusPath == STANDARD_MODEL_JSON || corpusPath.endsWith("/" + STANDARD_MODEL_JSON))) {
      corpusPath.dropRight(STANDARD_MODEL_JSON.length) + customModelJsonName
    } else corpusPath
  }

  /** Reverse redirect: convert paths ending in customModelJsonName back to "model.json" for the CDM SDK */
  private def toCorpusName(path: String): String = {
    if (path != null && (path == customModelJsonName || path.endsWith("/" + customModelJsonName))) {
      path.dropRight(customModelJsonName.length) + STANDARD_MODEL_JSON
    } else path
  }

  /** Delegates read capability check to the underlying ADLS adapter. */
  override def canRead(): Boolean = delegate.canRead()

  /** Delegates write capability check to the underlying ADLS adapter. */
  override def canWrite(): Boolean = delegate.canWrite()

  /** Reads file content, redirecting "model.json" requests to the custom filename. */
  override def readAsync(corpusPath: String): CompletableFuture[String] = {
    delegate.readAsync(toStoragePath(corpusPath))
  }

  /** Writes file content, redirecting "model.json" requests to the custom filename. */
  override def writeAsync(corpusPath: String, data: String): CompletableFuture[Void] = {
    delegate.writeAsync(toStoragePath(corpusPath), data)
  }

  /** Lists files, mapping customModelJsonName back to "model.json" in results for the SDK. */
  override def fetchAllFilesAsync(folderCorpusPath: String): CompletableFuture[JList[String]] = {
    delegate.fetchAllFilesAsync(folderCorpusPath).thenApply(files => {
      files.asScala.map(f => toCorpusName(f)).asJava
    })
  }

  /** Gets last-modified timestamp for the actual file (after redirecting the path). */
  override def computeLastModifiedTimeAsync(corpusPath: String): CompletableFuture[OffsetDateTime] = {
    delegate.computeLastModifiedTimeAsync(toStoragePath(corpusPath))
  }

  /** Converts corpus path to ADLS URL, redirecting so it points to the real file. */
  override def createAdapterPath(corpusPath: String): String = {
    // Redirect so the ADLS URL points to the custom file, not model.json
    delegate.createAdapterPath(toStoragePath(corpusPath))
  }

  /** Converts ADLS URL to corpus path, replacing customModelJsonName with "model.json" for the SDK. */
  override def createCorpusPath(adapterPath: String): String = {
    // If the adapter path resolves to the custom file, present it as "model.json" to the SDK
    val corpusPath = delegate.createCorpusPath(adapterPath)
    if (corpusPath != null) toCorpusName(corpusPath) else corpusPath
  }
}
