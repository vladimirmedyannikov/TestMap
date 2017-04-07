package ru.medyannikov.testmap.pojo

import com.fasterxml.jackson.annotation.JsonProperty

class RoutesItem {
	val summary: String? = null
	val copyrights: String? = null
	val legs: List<LegsItem?>? = null
	val warnings: List<Any?>? = null
	val bounds: Bounds? = null
	@JsonProperty("overview_polyline")
	val overviewPolyline: OverviewPolyline? = null
	@JsonProperty("waypoint_order")
	val waypointOrder: List<Int?>? = null
}
