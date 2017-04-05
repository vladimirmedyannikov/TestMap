package ru.medyannikov.testmap

import retrofit2.http.POST
import retrofit2.http.QueryMap
import ru.medyannikov.testmap.pojo.DirectionResponse
import rx.Observable

interface GoogleApi {

  companion object {
    const val DIRECTION_API_JSON = "/maps/api/directions/json"
  }

  @POST(DIRECTION_API_JSON)
  fun getDirectionApi(@QueryMap params: Map<String, String>) : Observable<DirectionResponse>
}