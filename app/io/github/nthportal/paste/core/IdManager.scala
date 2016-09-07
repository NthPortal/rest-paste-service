package io.github.nthportal.paste.core

import java.security.SecureRandom
import java.util
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}

import models.{IdPair, PasteData, PasteWriteData}
import org.apache.commons.codec.binary.{Base64, Hex}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.jdbc.JdbcBackend

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class IdManager @Inject() private[core](db: JdbcBackend#DatabaseDef) {
  import IdManager._

  private val random = new SecureRandom
  private val readIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- db.run(PasteData.ids)
      _ = set.addAll(seq)
    } yield set
  }
  private val writeIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- db.run(PasteWriteData.ids)
      _ = set.addAll(seq)
    } yield set
  }

  def randomRevisionId: String = {
    val bytes = Array.ofDim[Byte](revisionIdBytes)
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
    val bytes = Array.ofDim[Byte](idPairBytes)
    random.nextBytes(bytes)
    val str = Base64.encodeBase64URLSafeString(bytes)
    IdPair(str.substring(0, readIdLength), str.substring(readIdLength))
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
  private val maxAttempts = 20

  private val revisionIdBytes = 16
  private val idPairBytes = 12

  val readIdLength = 6
  val writeIdLength = 10
  val revisionIdLength = revisionIdBytes * 2

  // Check that id lengths are valid
  {
    val idLengthSum = readIdLength + writeIdLength
    assert(idLengthSum % 4 == 0, "Sum of RW id lengths must be a multiple of 4; is actually " + idLengthSum)
    val lengthBytes = idLengthSum / 4 * 3
    assert(lengthBytes == idPairBytes, "Sum of RW id lengths must match the number of bytes in an id pair; " +
      "id length sum bytes (" + lengthBytes + ") != id pair bytes (" + idPairBytes + ")")
  }
}
