# nextwork-admin-service

### Description :- 
    admin service is service in which manages user mangement,role management,workstream management and Database mangement

    1.User Mangement- Under this admin can onboard the users,delete the users and also can assign the roles.
    2.Role Management- Under this admin can create, edit and delete the roles. 
    3.Workstream Mangement-Under this admin can download KPIs,manage the workstream like edit,delete,copy, assign the workstream to a user
      create the report for intial analysis and implementation
    4.Database Management-Under this admin can upload master scoping GIDs data through manually and via excel, download the scopped version 
      and view all the versions data.


### API Info :-
| S No. | Interface Id | Name                              | URL                                 | Method | Description                                                                                                                                                                                   |
|-------|--------------|-----------------------------------|-------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     |              | createRequest                     | /scopes/async/{versionId}           | POST   | This endpoint is use to upload master data for any specific version and required parameters are versionId.                                                                                    |
| 2     |              | getStatusForAddJobProfilesProcess | /scopes/async/{asyncJobId}          | GET    | This endpoint is use to upload master data for any specific version and required parameters are asyncJobId.                                                                                   |
| 3     |              | appendScopeRequestviaExcel        | /scopes/async/{versionId}           | PUT    | This endpoint is use to Append scoping master data for any specific version via excel upload and required parameters are versionId..                                                          |
| 4     |              | appendScopeRequestviaTool         | /scopes/{versionId}/data            | PUT    | This endpoint is use to Append single or multiple scoping master data for any specific version. via tool. GID supplied in request body must be unique and required parameters are versionId.. | 
| 5     |              | updateGID                         | /scopes/{version_id}/{gid}          | PUT    | This endpoint is use to update any existing gid data in master DB Or Unpublish or Publish and required parameters are versionId, gId.                                                         |
| 6     |              | searchGID                         | /scopes/{version_id}/{gid}          | GET    | This endpoint is use to upload master data for any specific version and required parameters are versionId, gId.                                                                               |
| 7     |              | getAllVersions                    | /scopes/versionList                 | GET    | This endpoint is use to view list of all DB versions exists.                                                                                                                                  |
| 8     |              | publishUnpublishScopingMasterData | /scopes/{version_id}/action         | PUT    | This endpoint is use to publish or unpublish scoping master data for any specific version via tool and required parameters are versionId.                                                     |
| 9     |              | getVersionSummary                 | /scopes/{versionId}/summary         | GET    | This endpoint is use to view version summary and required parameters are versionId.                                                                                                           |
| 10    |              | addNote                           | /scopes/{version_id}/notes          | PUT    | This endpoint is use to add or append note. User can not append note created by others and required parameters are versionId.                                                                 |
| 11    |              | getNotes                          | /scopes/{version_id}/notes          | GET    | This endpoint is use to view notes of any version summary and required parameters are versionId                                                                                               |
| 12    |              | downloadScopingVersion            | /scopes/{versionId}/download        | GET    | This endpoint is use to to download master data based on version id and required parameters are versionId                                                                                     |
| 13    |              | createRole                        | /roles                              | POST   | This endpoint is use to create new role                                                                                                                                                       |
| 14    |              | updateRole                        | /roles/{roleId}                     | PUT    | This endpoint is use to Modify existing role like renaming display name, changing role type or add/remove GID scope or members and required parameters are roleId.                            |
| 15    |              | getAllRoles                       | /roles                              | GET    | This endpoint is use to Use this endpoint to get all defined roles or any specific role                                                                                                       |
| 16    |              | getRoleById                       | /roles/{id}                         | GET    | This endpoint is use to upload master data for any specific version and required parameters are roleId                                                                                        |
| 17    |              | getRoleDataBasedOnSearchQuery     | /roles/search                       | GET    | This endpoint is use to get all defined roles or any specific role and required parameters are searchQuery.                                                                                   |
| 18    |              | deleteRoles                       | /roles                              | PATCH  | This endpoint is use to upload master data for any specific version                                                                                                                           |
| 19    |              | download Skills Catalogue         | /skillsCatalogue/download           | GET    | This endpoint is use to Upload Master scoping data for any specific version                                                                                                                   |
| 20    |              | Get Upload File Status            | /uploadData/status                  | GET    | This endpoint is use to Upload Master scoping data for any specific version and required parameter is type                                                                                    |
| 21    |              | Update Project                    | /asyncExcelUpload/{id}              | PUT    | This endpoint is use to Upload Master scoping data for any specific version and required parameters are target,upload,id.                                                                     |
| 22    |              | Get Async JobStatusById           | /asyncJobStatus/{asyncJobId}        | GET    | This endpoint is use to Upload Master scoping data for any specific version and required parameters are asyncJobId.                                                                           |
| 23    |              | Create Access Request             | /accessRequest                      | POST   | This endpoint is use to Raise Access Request to NextWork tool                                                                                                                                 |
| 24    |              | getProjectKPIAsExcel              | /projectKpiDownload                 | POST   | This endpoint is use to Download project KPIs as excel                                                                                                                                        |
| 25    |              | getUserDetails                    | /users                              | GET    | This endpoint is use to Get List of all Users (restricted to Admin only) and required parameter is tabType.                                                                                   |
| 26    |              | getUserInfo                       | /userInfo                           | GET    | This endpoint is use to Endpoint to get basic info of any user (Available for all roles).                                                                                                     |
| 27    |              | updateUser                        | /users/{id}                         | PUT    | This endpoint is use to Change user's state, assigned role, workstream or gid (restricted to Admin only) and required parameter is id.                                                        |
| 28    |              | getUserById                       | /users/{id}                         | GET    | This endpoint is use to Get all details of specific Users (restricted to Admin only) and required parameter is id.                                                                            |
| 29    |              | asyncExcelUploadByType            | /upload/asyncExcelUpload            | POST   | This endpoint is use to Asynchronously Upload Master data for skill catalogue or GRIP task (restricted to Admin only) and required parameter is type.                                         |
| 30    |              | asyncJobStatusById                | /upload/asyncJobStatus/{asyncJobId} | GET    | This endpoint is use to Get Status of submitted Async job by using asyncJobId (restricted to Admin only) and required parameter is asyncJobId.                                                |
| 31    |              | addAnnouncement                   | /announcement                       | PUT    | This endpoint is use to Add an announcement (restricted to Admin only)                                                                                                                        |
| 32    |              | getAllAnnouncements               | /announcement                       | GET    | This endpoint is use to View list of Announcements and required parameter is                                                                                                                  |

   
    for more information about api's are in the below swagger document link
https://code.siemens.com/blrise/productive/nextwork/documentation/-/blob/master/AdminManagementService.yaml?ref_type=heads

### Dependencies :-
    1. OpenJdk 17
    2. Maven 3.9.x
    3. MongoDb 
   

### Local setup guideline for startup :-
```shell
git clone https://code.siemens.com/blrise/productive/nextwork/nextwork-new-admin-service.git
```
use following command to fetch all branches 

```shell
git fetch
```
Checkout to development branch

```shell
git checkout development
```
Build the application following command without running tests
```shell
mvn clean install -DskipTests
```
Build application using following command with running tests
```shell
mvn clean install
```
Once the above step is successful. It's time to run the application.

There are several ways to run a Spring Boot application on your local machine. One way is to execute the `main` method in the `com.siemens.nextwork.admin.AdminManagementServiceApplication` class from your IDE.

Alternatively you can use the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html) like so:

```shell
mvn spring-boot:run
```
#### MongoDB Configuration
Download latest mongoDb- https://www.mongodb.com/try/download/community and mongo compass -https://www.mongodb.com/try/download/compass

connect to localhost and create a database name as nextworkdb
under nextworkdb create below collections

1.Roles_Data

2.Users_data

inside Roles_Data create a manual entry in the below format
```
[{
"_id": {
"$oid": "64a52b534febce0ce703981b"
},
"roleDisplayName": "Admin",
"roleDescription": "Admin",
"roleType": "ADMIN",
"createdBy": "your email@siemens.com",
"createdByEmail": "xyz@siemens.com",
"time": {
"$date": "2023-07-10T06:24:20.752Z"
},
"haveGIDList": false,
"isDeleted": false,
"_class": "com.siemens.nextwork.project.model.Roles"
}]
```
inside Users_data create a manual entry in the below format
```
[{
"_id": {
"$oid": "64b0e557e3280a909b3a3955"
},
"name": "User Name",
"email": "User Email@siemens.com",
"org_code": "XYZ",
"created_on": "2022-10-19 06:53:12.243+00",
"rolesDetails": [
{
"_id": {
"$oid": "64a52b534febce0ce703981b"
},
"roleDisplayName": "Admin",
"roleType": "ADMIN",
"haveGIDList": false,
"isDeleted": false
}
],
"status": "Active",
"haveGIDList": false,
"_class": "com.siemens.nextwork.admin.model.NextWorkUser"
}]
```
Note: we are running the application in IDE main method.
### Version :- 
    1.0



