package io.github.nthportal.paste.core

import java.security.SecureRandom
import java.util
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}

import models.{PasteData, PasteWriteData}
import org.apache.commons.codec.binary.{Base64, Hex}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class IdManager @Inject()(manager: Manager) {
  private val maxAttempts = 20
  private val random = new SecureRandom
  private val readIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- manager.db.run(PasteData.ids)
      _ = set.addAll(seq)
    } yield set
  }
  private val writeIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- manager.db.run(PasteWriteData.ids)
      _ = set.addAll(seq)
    } yield set
  }

  def randomRevisionId: String = {
    val bytes = Array.ofDim[Byte](16)
    random.nextBytes(bytes)
    Hex.encodeHexString(bytes)
  }

  /**
    * Returns a pair of base64 IDs of lengths (6, 10), to be used
    * for read and write IDs respectively.
    *
    * @return a pair of random IDs
    */
  private def randomIdPair: IdPair = {
    val bytes = Array.ofDim[Byte](12)
    random.nextBytes(bytes)
    val str = Base64.encodeBase64URLSafeString(bytes)
    IdPair(str.substring(0, 6), str.substring(6))
  }

  /**
    * Returns a pair of base64 IDs of lengths (6, 10), to be used
    * for read and write IDs respectively. Keeps generating pairs
    * until neither is currently in use.
    *
    * @return a [[Future]] containing a pair of random IDs
    */
  def unusedRandomIdPair(): Future[IdPair] = {
    for {
      readIdSet <- readIds
      writeIdSet <- writeIds
    } yield unusedRandomIdPair(0, readIdSet, writeIdSet)
  }

  @tailrec
  private def unusedRandomIdPair(attempts: Int, readIds: util.Set[String], writeIds: util.Set[String]): IdPair = {
    if (attempts > maxAttempts) throw new RuntimeException("Too many attempts to generate an unused random ID pair")

    val pair = randomIdPair
    if (readIds add pair.readId) {
      if (writeIds add pair.writeId) {
        pair
      } else {
        readIds remove pair.readId
        unusedRandomIdPair(attempts + 1, readIds, writeIds)
      }
    } else {
      unusedRandomIdPair(attempts + 1, readIds, writeIds)
    }
  }
}

object IdManager {
  val readIdLength = 6
  val writeIdLength = 10
  val revisionIdLength = 32
}
