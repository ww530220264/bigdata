package com.ww.spark.rdd

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

object Spark_1_Rdd_DataFrame_DataSet {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .master("local[3]")
      .appName("app_1")
      .getOrCreate()
    // 根据Json数据获取DataFrame
    val fileDataFrame = spark.read.json("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\2015-03-01-1.json")
    // 打印DataFrame的schema信息
    fileDataFrame.printSchema()
    println("all events: " + fileDataFrame.count)
    val pushs = fileDataFrame.filter("type = 'PushEvent'")
    pushs.show(3)
    println("push events: " + pushs.count)
    // groupBy
    val grouped = pushs.groupBy("actor.login").count
    grouped.show(5)
    // 排序_1
    val ordered_1 = grouped.orderBy("login", "count")
    ordered_1.show(5)
    // 排序_2
    val ordered_2 = grouped.orderBy(grouped("count").desc, grouped("login"))
    ordered_2.cache()
    ordered_2.show(5)
    // 加载员工姓名文件
    import scala.io.Source.fromFile
    val emPath = "E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\getEmployees.txt"
    val employees = Set() ++ (
      for {
        line <- fromFile(emPath).getLines()
      } yield line.trim
      )
    val isEmp: (String => Boolean) = (arg: String) => employees.contains(arg)
    val isEmployee = spark.udf.register("isEmployeeUDF", isEmp)
    // 导入隐式转换
    import spark.implicits._
    // 使用filter过滤
    ordered_2.filter(isEmployee($"login")).show(6)
    // 或者使用where子句调用UDF
    ordered_2.where("isEmployeeUDF(login)").show(7)
    // 使用广播变量
    val broadcastEmplpyees = spark.sparkContext.broadcast(employees)
    val isEmp_2: (String => Boolean) = (arg: String) => broadcastEmplpyees.value.contains(arg)
    val isEmployee_2 = spark.udf.register("isEmployeeUDF_2", isEmp_2)
    ordered_2.filter(isEmployee_2($"login")).show(8)
    ordered_2.where("isEmployeeUDF_2(login)").show(9)

    // Rdd转DataFrame
    val employeeCsv = spark.sparkContext.textFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\hr_employ.csv")
    val schemaStr = "name,username,sex,borndate,height,idcard,orgName"
    val fields = schemaStr.split(",").map(fieldname => StructField(fieldname, StringType))
    val schema = StructType(fields)
    val empRdd = employeeCsv.map(_.split(","))
    val employeeRowRdd = empRdd.map(x => Row(x(0), x(1), x(2), x(3), x(4), x(5), x(6)))
    // 使用RDD[Row]和schema创建DataFrame
    val employeeDF_1 = spark.createDataFrame(employeeRowRdd, schema)
    employeeDF_1.printSchema()
    employeeDF_1.show(10)
// 报错
//        val employeeDF_2 = empRdd.toDF(Seq("name","username","sex","borndate","height","idcard","orgName"):_*)
//        employeeDF_2.printSchema()
//        employeeDF_2.show(15)

    // 使用case class和toDF创建DataFrame
    val employeeDF_3 = empRdd.map(x => Employee(x(0), x(1), x(2), x(3), x(4), x(5), x(6)))
      .toDF()
    employeeDF_3.printSchema()
    employeeDF_3.show(20)
    employeeDF_3
      .write
      .format("csv")
      .save("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\hr_employ_back.csv")
  }
}

case class Employee(var name: String,
                    var username: String,
                    var sex: String,
                    var borndate: String,
                    var height: String,
                    var idcard: String,
                    var orgName: String)