package com.siemens.nextwork.admin.service;

import java.util.List;

public interface SkillCatalogueService {

	List<String> getSkillsCataloguePreSignedUrl(String userEmail);

}
