package io.github.nthportal.paste.core

import java.security.SecureRandom
import java.util
import java.util.concurrent.ConcurrentHashMap

import io.github.nthportal.paste.data.{PasteReadData, PasteWriteData}
import org.apache.commons.codec.binary.Base64

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

object IdManager {
  private val maxAttempts = 20
  private val random = new SecureRandom
  private val db = Manager.config.db
  private val readIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- db.run(PasteReadData.ids)
      set.addAll(seq)
    } yield set
  }
  private val writeIds: Future[util.Set[String]] = {
    val set = ConcurrentHashMap.newKeySet[String]()
    for {
      seq <- db.run(PasteWriteData.ids)
      set.addAll(seq)
    } yield set
  }

  /**
    * Returns a pair of base64 IDs of lengths (6, 10), to be used
    * for read and write IDs respectively.
    *
    * @return a pair of random IDs
    */
  private def randomIdPair: (String, String) = {
    val bytes = Array.ofDim[Byte](12)
    random.nextBytes(bytes)
    val str = Base64.encodeBase64URLSafeString(bytes)
    (str.substring(0, 6), str.substring(6))
  }

  /**
    * Returns a pair of base64 IDs of lengths (6, 10), to be used
    * for read and write IDs respectively. Keeps generating pairs
    * until neither is currently in use.
    *
    * @return a [[Future]] containing a pair of random IDs
    */
  def unusedRandomIdPair(implicit ec: ExecutionContext): Future[(String, String)] = {
    unusedRandomIdPair(0)
  }

  private def unusedRandomIdPair(attempts: Int)(implicit ec: ExecutionContext): Future[(String, String)] = {
    val pair = randomIdPair
    for {
      readIdSet <- readIds
      if readIdSet add pair._1
      validReadId <- {
        for {
          writeIdSet <- writeIds
          if writeIdSet add pair._2
        } yield true
      } recoverWith {
        case _ =>
          readIdSet remove pair._1
          Future.failed(null)
      }
    } yield pair
  } recoverWith {
    case _ if attempts < maxAttempts => unusedRandomIdPair(attempts + 1)
    case _ => throw new RuntimeException("Too many attempts to generate an unused random ID pair")
  }
}
