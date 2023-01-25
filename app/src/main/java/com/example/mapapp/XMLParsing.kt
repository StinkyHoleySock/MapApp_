package com.example.mapapp

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.util.*

fun parseFile(context: Context, fileName: String): MutableList<Point> {

    val file = (File(context.filesDir, fileName))
    val input = FileInputStream(file)
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(input, null)

    var eventType = parser.eventType

    var missionId = ""
    var latitude = ""
    var longitude = ""
    var time = ""
    val points: MutableList<Point> = mutableListOf()

    while (eventType != XmlPullParser.END_DOCUMENT) {

        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "mission" -> {
                        missionId = parser.getAttributeValue("", "id") ?: throw Exception()
                    }
                    "point" -> {
                        latitude = parser.getAttributeValue("", "latitude") ?: throw Exception()
                        longitude = parser.getAttributeValue("", "longitude") ?: throw Exception()
                        time = parser.getAttributeValue("", "time") ?: throw Exception()
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "quest" -> {Mission(missionId, points)}
                    "point" -> {points.add(Point(latitude, longitude, time))}
                }
            }
        }

        eventType = parser.next()
    }
    input.close()

    return points
}