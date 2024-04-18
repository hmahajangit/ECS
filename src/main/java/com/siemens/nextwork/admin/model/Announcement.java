package com.siemens.nextwork.admin.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "announcement")
public class Announcement {
	
	@Id
	private String id;
	private String title;
	private String description;
    @Field(name = "created_by")
    private String createdBy;
    @Field(name = "created_on")
    protected Date createdOn;
    @Field(name = "updated_by")    
    private String updatedBy;
    @Field(name = "updated_on")
    protected Date updatedOn;
    private Integer version;
    private Boolean deleted;
}
