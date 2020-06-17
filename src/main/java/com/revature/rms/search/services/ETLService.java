package com.revature.rms.search.services;

import com.revature.rms.search.clients.CampusClient;
import com.revature.rms.search.clients.EmployeeClient;
import com.revature.rms.search.dtos.*;
import com.revature.rms.search.entites.batch.Batch;
import com.revature.rms.search.entites.campus.*;
import com.revature.rms.search.entites.common.ResourceMetadata;
import com.revature.rms.search.entites.employee.Employee;
import com.revature.rms.search.entites.workorder.WorkOrder;
import com.revature.rms.search.exceptions.InvalidRequestException;
import com.revature.rms.search.exceptions.ResourceNotFoundException;
import com.revature.rms.search.repositories.BatchRepository;
import com.revature.rms.search.repositories.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In the first iteration of this service, we tried
 * to make each phase of the transformation process
 * as readable as possible. To make it a easier to
 * keep track of how it was achieved, we grouped each
 * set of the methods that were related to each other
 * together so that there was less jumping around to
 * see what each method took care of. Orchestrating
 * the calls to each service was carefully planned
 * to ensure that no pieces were missing before
 * delivering the final DTO to the front-end.
 * More validation will be needed to account for empty
 * or null objects that may be received from the other
 * services.
 * */
@Service
public class ETLService {

  private EmployeeClient empClient;
  private CampusClient campClient;
  private WorkOrderRepository workRepo;
  private BatchRepository batchRepo;

  /**
   * The repositories will need to be changed
   * to the feign clients in the next sprint
   * */
  @Autowired
  public ETLService(
      EmployeeClient employeeClient,
      CampusClient campusClient,
      WorkOrderRepository workOrderRepository,
      BatchRepository batchRepository) {
    super();
    this.empClient = employeeClient;
    this.campClient = campusClient;
    this.workRepo = workOrderRepository;
    this.batchRepo = batchRepository;
  }

  //****************************** Campus Services ********************************************
  /**
   * getAllCampuses method: Returns all CampusDto object with all nested objects
   * @param
   * @return a list of CampusDto objects
   * @throws InvalidRequestException when a bad request is made
   */
  public List<CampusDto> getAllCampuses() {
    List<CampusDto> dtos = new ArrayList<>();
    try {
      List<Campus> campuses = campClient.getAllCampus();
      campuses.forEach(c -> dtos.add(getCampusDto(c)));
    } catch (Exception e) {
      throw new InvalidRequestException("Bad request made!");
    }
    return dtos;
  }

  /**
   * getCampusDto method: Returns a CampusDto object with all nested objects after recieving a campus object without nested objects complete
   * @param campus
   * @return a CampusDto object
   * @throws ResourceNotFoundException building or meta data is not found
   */
  public CampusDto getCampusDto(Campus campus) {
    CampusDto dto = getCampusObjects(campus);
    try{
      dto.setBuildings(getListOfBuildingsData(campus.getBuildings()));
      dto.setCorporateEmployees(
              getEachEmployeeMeta(empClient.getAllById(campus.getCorporateEmployees())));
    }catch(Exception e){
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  /**
   * getCampusDtoById method: Returns a CampusDto object with all nested objects
   * @param id
   * @return a CampusDto object
   * @throws ResourceNotFoundException when the campus, buildings or metadata cannot be found
   */
  public CampusDto getCampusDtoById(String id) {
    CampusDto campusDto = new CampusDto();
    try {
      Campus campus = campClient.getCampusById(id);
      campusDto = getCampusObjects(campus);
      campusDto.setBuildings(getListOfBuildingsData(campus.getBuildings()));
      campusDto.setCorporateEmployees(
              getEachEmployeeMeta(empClient.getAllById(campus.getCorporateEmployees())));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return campusDto;
  }

  /**
   * getCampusObjects method: Returns a CampusDto object with all nested objects after recieving a campus object without nested objects complete
   * @param campus
   * @return a CampusDto object
   * @throws ResourceNotFoundException building or meta data is not found
   */
  public CampusDto getCampusObjects(Campus campus) {
    CampusDto dto = campus.extractCampus();
    try {
      dto.setTrainingManager(getEmployeeById(campus.getTrainingManagerId()));
      dto.setStagingManager(getEmployeeById(campus.getStagingManagerId()));
      dto.setHrLead(getEmployeeById(campus.getHrLead()));
      dto.setResourceMetadata(campusMetaData(campus.getResourceMetadata()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  /**
   * campusMetaData method: Returns a ResourceMetadataDto object with all nested objects after recieving a campus object without nested objects complete
   * @param data
   * @return a ResourceMetadataDto object
   * @throws ResourceNotFoundException if metadata is not found
   */
  public ResourceMetadataDto campusMetaData(ResourceMetadata data) {
    ResourceMetadataDto dto = data.extractResourceMetadata();
    try {
      dto.setResourceCreator(getEmployeeDtoById(data.getResourceCreator()));
      dto.setLastModifier(getEmployeeDtoById(data.getLastModifier()));
      dto.setResourceCreator(getEmployeeDtoById(data.getResourceOwner()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  /**
   * getListOfBuildingsData method: Returns all BuildingDto object with all nested objects
   * @param buildings
   * @return a list of BuildingDto objects
   * @throws ResourceNotFoundException if no building object is found
   */
  public List<BuildingDto> getListOfBuildingsData(List<Building> buildings){
    List<BuildingDto> buildingDtos = new ArrayList<>();
    try {
      buildings.forEach(b -> buildingDtos.add(b.extractBuilding()));
      for (int i = 0; i < buildings.size(); i++) {
        Building building = buildings.get(i);
        buildingDtos.get(i).setTrainingLead(getEmployeeById(building.getTrainingLead()));
        buildingDtos.get(i).setRooms(getEachRoomMeta(building.getRooms()));
        if(building.getResourceMetadata() != null){
          buildingDtos.get(i).setResourceMetadata(campusMetaData(building.getResourceMetadata()));
        }
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return buildingDtos;
  }

  /**
   * getBuildingDtoById method: Returns a BuildingDto object with all nested objects
   * @param id
   * @return a  BuildingDto object
   * @throws ResourceNotFoundException when the campus, buildings or metadata cannot be found
   */
  public BuildingDto getBuildingDtoById(String id) {
    try {
      Building building = campClient.getBuildingById(id);
      return getBuildingData(building);
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
  }

  /**
   * getBuildingData method: Returns a CampusDto object with all nested objects
   * @param building
   * @return a BuildingDto object
   * @throws ResourceNotFoundException when the BuildingDto cannot be found
   */
  public BuildingDto getBuildingData(Building building) {
    BuildingDto dto = building.extractBuilding();
    try {
      dto.setTrainingLead(getEmployeeById(building.getTrainingLead()));
      dto.setRooms(getEachRoomMeta(building.getRooms()));
      // Campus object received from campus service returned null metadata for the building objects
        // so I put this validation in here to just skip over it. Will need to investigate why this is happening
      if(building.getResourceMetadata() != null){
        dto.setResourceMetadata(campusMetaData(building.getResourceMetadata()));
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  /**
   * getRoomDtoById method: Returns a RoomDto object with all nested objects
   * @param id
   * @return a RoomDto object
   * @throws ResourceNotFoundException when the RoomDto cannot be found
   */
  public RoomDto getRoomDtoById(String id) {
    RoomDto roomDto = new RoomDto();
    try {
      Room room = campClient.getRoomById(id);
      roomDto = room.extractRoom();
      roomDto.setResourceMetadata(campusMetaData(room.getResourceMetadata()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return roomDto;
  }

  /**
   * campusMetaData method: Returns list of  RoomDto object with all nested objects after recieving a campus object without nested objects complete
   * @param rooms
   * @return a RoomDto object
   * @throws ResourceNotFoundException if metadata is not found
   */
  public List<RoomDto> getEachRoomMeta(List<Room> rooms){
    List<RoomDto> roomDtos = new ArrayList<>();
    try {
      rooms.forEach(r -> roomDtos.add(r.extractRoom()));
      for (int i = 0; i < rooms.size(); i++) {
        Room room = rooms.get(i);
        roomDtos.get(i).setCurrentStatus(getEmpsFromRoomStatus(room.getRoomStatus()));
        roomDtos.get(i).setResourceMetadata(campusMetaData(room.getResourceMetadata()));
        roomDtos.get(i).setBatch(getBatchInfo(findBatchById(room.getBatchId())));
        roomDtos.get(i).setWorkOrders(getEachWorkOrderInfo(room.getWorkOrders()));
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return roomDtos;
  }

  /**
   * getEmpsFromRoomStatus method: Returns all RoomStatusDto object with employee associated with RoomStatusDto
   * @param roomStatus
   * @return a list of RoomStatusDto objects
   * @throws ResourceNotFoundException if no RoomStatusDto object is found
   */
  public List<RoomStatusDto> getEmpsFromRoomStatus(List<RoomStatus> roomStatus) {
    List<RoomStatusDto> dtos = new ArrayList<>();
    try {
      for (int i = 0; i < roomStatus.size(); i++) {
        RoomStatus status = roomStatus.get(i);
        RoomStatusDto statusDto = status.extractRoomStatus();
        statusDto.setSubmitter(getEmployeeById(status.getSubmitterId()));
        dtos.add(statusDto);
      }
    } catch (Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dtos;
  }

  //****************************** Employee Services ********************************************
  /**
   * getAllEmployees method: Returns all EmployeeDto object with all nested objects
   * @param
   * @return a list of employee objects
   */
  public List<EmployeeDto> getAllEmployees() {
    return getEachEmployeeMeta(empClient.getAllEmployee());
  }

  /**
   * Although, this method looks exactly the same as the one
   * under it, it was necessary to create them separately to
   * avoid circular references made to the employee service.
   * */

  /**
   * getEmployeeDtoById method: Returns a EmployeeDto object with all nested objects
   * @param id
   * @return a list of employee objects
   * @throws ResourceNotFoundException if EmployeeDto object is not found
   */
  public EmployeeDto getEmployeeDtoById(int id) {
    EmployeeDto employeeDto = new EmployeeDto();
    try {
      Employee employee = empClient.getEmployeeById(id);
      employeeDto = employee.extractEmployee();
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return employeeDto;
  }

  /**
   * getEmployeeDtoById method: Set an EmployeeDto ResourceMetadata
   * @param id
   * @return an EmployeeDto object
   * @throws ResourceNotFoundException if ResourceMetadata object is not found
   */
  public EmployeeDto getEmployeeById(int id) {
    EmployeeDto dto = new EmployeeDto();
    try {
      Employee emp = empClient.getEmployeeById(id);
      dto = emp.extractEmployee();
      dto.setResourceMetadata(getEmployeeMetadata(emp.getResourceMetadata()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  /**
   * getEachEmployeeMeta method: Set an EmployeeDto ResourceMetadata for all EmployeeDto in list
   * @param employees
   * @return a list of employee objects
   * @throws ResourceNotFoundException if ResourceMetadata object is not found
   */
  public List<EmployeeDto> getEachEmployeeMeta(List<Employee> employees){
    List<EmployeeDto> empDtos = new ArrayList<>();
    try {
      employees.forEach(e -> empDtos.add(e.extractEmployee()));
      for (int i = 0; i < employees.size(); i++) {
        Employee emp = employees.get(i);
        empDtos.get(i).setResourceMetadata(getEmployeeMetadata(emp.getResourceMetadata()));
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return empDtos;
  }

  /**
   * getEmployeeMetadata method: Returns a ResourceMetadataDto object with all nested objects after recieving an employee object without nested objects complete
   * @param data
   * @return a resource metadata object
   * @throws ResourceNotFoundException if metadata is not found
   */
  public ResourceMetadataDto getEmployeeMetadata(
      com.revature.rms.search.entites.employee.ResourceMetadata data) {
    ResourceMetadataDto dto = data.extractEmployeeMeta();
    try {
      dto.setResourceCreator(getEmployeeDtoById(data.getResourceCreator()));
      dto.setLastModifier(getEmployeeDtoById(data.getLastModifier()));
      dto.setResourceOwner(getEmployeeDtoById(data.getResourceOwner()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }

  //****************************** Work Order Services ********************************************
  /**
   * These methods will need to be updated when
   * batch service and work order service are
   * completed.
   * */
  @Transactional
  public WorkOrder getWorkOrderById(String id) {
    WorkOrder w = new WorkOrder();
    try {
      Optional<WorkOrder> workOrder = workRepo.findById(id);
      if (workOrder.isPresent()) {
        w = workOrder.get();
      } else {
        return w;
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return w;
  }

  public List<WorkOrderDto> getEachWorkOrderInfo(List<String> ids) {
    List<WorkOrderDto> dtos = new ArrayList<>();
    try {
      List<WorkOrder> workOrders = new ArrayList<>();
      ids.forEach(i -> workOrders.add(getWorkOrderById(i)));
      for (int j = 0; j < workOrders.size(); j++) {
        WorkOrderDto dto = workOrders.get(j).extractWorkOrder();
        dto.setCreator(getEmployeeById(workOrders.get(j).getCreatorId()));
        dto.setResolver(getEmployeeById(workOrders.get(j).getResolverId()));
        dtos.add(dto);
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dtos;
  }

  //****************************** Batch Services ********************************************
  @Transactional
  public Batch findBatchById(String id) {
    Batch b = new Batch();
    try {
      Optional<Batch> batch = batchRepo.findById(id);
      if (batch.isPresent()) {
        b = batch.get();
      } else {
        return b;
      }
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return b;
  }

  public BatchDto getBatchInfo(Batch batch) {
    BatchDto dto = batch.extractBatch();
    try {
      dto.setTrainer(getEmployeeById(batch.getTrainerId()));
      if (batch.getCoTrainerId() != 0) {
        dto.setCoTrainer(getEmployeeById(batch.getCoTrainerId()));
      }
      dto.setAssociates(getEachEmployeeMeta(empClient.getAllById(batch.getAssociates())));
      dto.setResourceMetadata(campusMetaData(batch.getResourceMetadata()));
    }catch(Exception e) {
      throw new ResourceNotFoundException("Resource not found!");
    }
    return dto;
  }
}
