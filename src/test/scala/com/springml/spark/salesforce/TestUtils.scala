/*
 * Copyright 2015 springml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.springml.spark.salesforce

import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.types.{StructType, StringType, StructField}
import org.scalatest.FunSuite
import org.apache.spark.SparkConf
import org.scalatest.BeforeAndAfterEach

class TestUtils extends FunSuite with BeforeAndAfterEach {
  var sparkConf: SparkConf = _
  var sc: SparkContext = _

  override def beforeEach() {
    sparkConf = new SparkConf().setMaster("local").setAppName("data repartition")
    sc = new SparkContext(sparkConf)
  }
     
  override def afterEach() {
    sc.stop()
  }
   
  test("Test Metadata generation") {
    val columnNames = List("c1", "c2", "c3", "c4")
    val columnStruct = columnNames.map(colName => StructField(colName, StringType, true))
    val schema = StructType(columnStruct)

    val schemaString = Utils.generateMetaString(schema,"sampleDataSet")
    assert(schemaString.length > 0)
    assert(schemaString.contains("sampleDataSet"))
  }

  test("Test repartition for in memory RDD") {
    val sqlContext = new SQLContext(sc)

    val inMemoryData = (0 to 2000).map(value => {
      val rowValues = Array(value, value + 1, value + 2, value + 3).map(value => value.toString)
      Row.fromSeq(rowValues)
    })

    val inMemoryRDD = sc.makeRDD(inMemoryData)
    val columnNames = List("c1", "c2", "c3", "c4")
    val columnStruct = columnNames.map(colName => StructField(colName, StringType, true))
    val schema = StructType(columnStruct)
    val inMemoryDF = sqlContext.createDataFrame(inMemoryRDD, schema)

    val repartitionDF = Utils.repartition(inMemoryRDD)
    assert(repartitionDF.partitions.length == 1)
  }

  test("Test repartition for local CSV file with size less than 10 MB") {
    val sqlContext = new SQLContext(sc)

    val csvURL= getClass.getResource("/ad-server-data-formatted.csv")
    val csvFilePath = csvURL.getPath
    val csvDF = sqlContext.
                        read.
                        format("com.databricks.spark.csv").
                        option("header", "true").
                        load(csvFilePath)

    val repartitionDF = Utils.repartition(csvDF.rdd)
    assert(repartitionDF.partitions.length == 1)
    sc.stop
  }

  test("Test repartition for local CSV file with size > 10 MB and < 20 MB") {
    val sqlContext = new SQLContext(sc)

    val csvURL= getClass.getResource("/minified_GDS_90.csv")
    val csvFilePath = csvURL.getPath
    val csvDF = sqlContext.
                        read.
                        format("com.databricks.spark.csv").
                        option("header", "true").
                        load(csvFilePath)

    val repartitionDF = Utils.repartition(csvDF.rdd)
    assert(repartitionDF.partitions.length == 2)
    sc.stop
  }

}