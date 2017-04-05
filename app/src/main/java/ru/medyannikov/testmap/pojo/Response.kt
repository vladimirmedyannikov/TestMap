package ru.medyannikov.testmap.pojo

import com.fasterxml.jackson.annotation.JsonProperty

class DirectionResponse {
  val routes: List<RoutesItem?>? = null

  @JsonProperty("geocoded_waypoints")
  val geocodedWaypoints: List<GeocodedWaypointsItem?>? = null

  val status: String? = null
}

