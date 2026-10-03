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
  fun strokeSerializer_deserializesValidStrokes() {
    val sampleJson = """[{"color":-12937736,"width":6.0,"tool":"PEN","points":[{"x":60.0,"y":70.0},{"x":85.0,"y":60.0}]}]"""
    val strokes = com.example.domain.model.StrokeSerializer.deserialize(sampleJson)
    assertEquals(1, strokes.size)
    assertEquals("PEN", strokes[0].tool)
    assertEquals(2, strokes[0].points.size)
  }
}
