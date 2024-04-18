package com.siemens.nextwork.admin.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(name = "AnnouncementDTO", description = "Data object for Announcement Request", oneOf = AnnouncementDTO.class)
public class AnnouncementDTO {

	private String uid;

	private String title;

	private String description;
	
	private LocalDate date;

}
