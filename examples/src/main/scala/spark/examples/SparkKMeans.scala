package spark.examples

import java.util.Random
import spark.SparkContext
import spark.util.Vector
import spark.SparkContext._

/**
 * K-means clustering.
 */
object SparkKMeans {
  val R = 1000     // Scaling factor
  val rand = new Random(42)
    
  def parseVector(line: String): Vector = {
      return new Vector(line.split(' ').map(_.toDouble))
  }
  
  def closestPoint(p: Vector, centers: Array[Vector]): Int = {
    var index = 0
    var bestIndex = 0
    var closest = Double.PositiveInfinity
  
    for (i <- 0 until centers.length) {
      val tempDist = p.squaredDist(centers(i))
      if (tempDist < closest) {
        closest = tempDist
        bestIndex = i
      }
    }
  
    return bestIndex
  }

  def main(args: Array[String]) {
    if (args.length < 4) {
        System.err.println("Usage: SparkLocalKMeans <master> <file> <k> <convergeDist>")
        System.exit(1)
    }
    val sc = new SparkContext(args(0), "SparkLocalKMeans",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))
    val lines = sc.textFile(args(1))
    val data = lines.map(parseVector _).cache()
    val K = args(2).toInt
    val convergeDist = args(3).toDouble
  
    var kPoints = data.takeSample(false, K, 42).toArray
    var tempDist = 1.0

    while(tempDist > convergeDist) {
      var closest = data.map (p => (closestPoint(p, kPoints), (p, 1)))
      
      var pointStats = closest.reduceByKey{case ((x1, y1), (x2, y2)) => (x1 + x2, y1 + y2)}
      
      var newPoints = pointStats.map {pair => (pair._1, pair._2._1 / pair._2._2)}.collectAsMap()
      
      tempDist = 0.0
      for (i <- 0 until K) {
        tempDist += kPoints(i).squaredDist(newPoints(i))
      }
      
      for (newP <- newPoints) {
        kPoints(newP._1) = newP._2
      }
    }

    println("Final centers:")
    kPoints.foreach(println)
    System.exit(0)
  }
}
