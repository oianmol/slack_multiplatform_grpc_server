package dev.baseio.slackserver.data

import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

object Database {
  private val client = KMongo.createClient(connectionString = System.getenv("connection.mongodb"))

  val slackDB = client.getDatabase("slackDB").coroutine //normal java driver usage
}