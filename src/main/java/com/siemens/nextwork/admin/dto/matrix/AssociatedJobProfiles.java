package com.siemens.nextwork.admin.dto.matrix;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssociatedJobProfiles {

	private List<String> statusQuoJPIds;
    private List<String> futureStateJPIds;
}
