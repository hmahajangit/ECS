package com.siemens.nextwork.admin.service;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestAccessDTO;

public interface RequestAccessService {

	IdResponseDTO createAccessRequest(RequestAccessDTO requestAccessDTO);

}
