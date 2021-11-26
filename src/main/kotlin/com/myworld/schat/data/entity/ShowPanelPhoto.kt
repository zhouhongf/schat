package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "show_panel_photo")
class ShowPanelPhoto(
    @Id
    @Field("_id")
    var id: String,
    @Field("file_name")
    var fileName: String,
    @Field("extension_type")
    var extensionType: String,
    @Field("file_byte")
    var fileByte: ByteArray
) : Serializable
