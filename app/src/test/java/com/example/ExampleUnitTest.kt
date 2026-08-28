package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMailTmGetMessages() = kotlinx.coroutines.runBlocking {
    val tokenResp = com.example.data.api.ApiClient.mailTmService.getToken(
      com.example.data.api.TokenRequest("mycustom98712@emalupe.com", "Password123!")
    )
    println("Token response: isSuccessful=${tokenResp.isSuccessful}, code=${tokenResp.code()}")
    val token = tokenResp.body()?.token ?: return@runBlocking
    println("Token: $token")
    try {
      val messagesResp = com.example.data.api.ApiClient.mailTmService.getMessages("Bearer $token")
      println("Messages response: isSuccessful=${messagesResp.isSuccessful}, code=${messagesResp.code()}, body=${messagesResp.body()}")
    } catch (e: Exception) {
      println("Messages EXCEPTION: ${e.javaClass.name}: ${e.message}")
      e.printStackTrace()
    }
  }

  @Test
  fun testGuerrillaMailFlow() = kotlinx.coroutines.runBlocking {
    try {
      val initResp = com.example.data.api.ApiClient.guerrillaMailService.getEmailAddress()
      println("Guerrilla init: code=${initResp.code()} body=${initResp.body()}")
      val sid = initResp.body()?.sidToken ?: return@runBlocking

      val setResp = com.example.data.api.ApiClient.guerrillaMailService.setEmailUser(
        emailUser = "mycustomtest9988",
        site = "sharklasers.com",
        sidToken = sid
      )
      println("Guerrilla set: code=${setResp.code()} body=${setResp.body()}")

      val listResp = com.example.data.api.ApiClient.guerrillaMailService.getEmailList(offset = 0, sidToken = sid)
      println("Guerrilla list: code=${listResp.code()} body=${listResp.body()}")

      val mailId = listResp.body()?.list?.firstOrNull()?.mailId
      if (mailId != null) {
        val detailResp = com.example.data.api.ApiClient.guerrillaMailService.fetchEmail(emailId = mailId, sidToken = sid)
        println("Guerrilla detail: code=${detailResp.code()} body=${detailResp.body()}")
      }
    } catch (e: Exception) {
      println("Guerrilla EXCEPTION: ${e.javaClass.name}: ${e.message}")
      e.printStackTrace()
      throw e
    }
  }
}
