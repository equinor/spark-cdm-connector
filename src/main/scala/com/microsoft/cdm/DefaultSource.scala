package com.microsoft.cdm

import org.apache.spark.sql.connector.catalog.{Table, TableProvider}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import org.slf4j.LoggerFactory
import com.microsoft.cdm.utils.CDMOptions


class DefaultSource extends TableProvider with DataSourceRegister {

    val logger  = LoggerFactory.getLogger(classOf[DefaultSource])

    override def inferSchema(options: CaseInsensitiveStringMap): StructType = {
        new HadoopTables().load(new CDMOptions(options)).schema
    }
    
    override def getTable(structType: StructType, transforms: Array[Transform], map: java.util.Map[String, String]): Table = {
        val caseInsensitiveStringMap = new CaseInsensitiveStringMap(map)
        val schema = if (structType != null) structType else inferSchema(caseInsensitiveStringMap)
        new SparkTable(schema, caseInsensitiveStringMap)
    }

    override def supportsExternalMetadata(): Boolean = true

    override def shortName(): String = "cdm"
}
