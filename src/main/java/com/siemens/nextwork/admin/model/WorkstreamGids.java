package com.siemens.nextwork.admin.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "workstream_gids")
public class WorkstreamGids {
    @Id
    private String id;
    @Indexed(unique = true)
    private String workstreamId;
    private List<String> gidList;
    private List<GidData> gidDataList;

}