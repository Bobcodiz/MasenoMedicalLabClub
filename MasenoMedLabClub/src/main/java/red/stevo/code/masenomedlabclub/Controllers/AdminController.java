package red.stevo.code.masenomedlabclub.Controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import red.stevo.code.masenomedlabclub.ControllerAdvice.custom.EntityDeletionException;
import red.stevo.code.masenomedlabclub.Models.RequestModels.IndexPageImageModel;
import red.stevo.code.masenomedlabclub.Models.RequestModels.UsersRegistrationRequests;
import red.stevo.code.masenomedlabclub.Models.RequestModels.events.EventsCreationRequest;
import red.stevo.code.masenomedlabclub.Models.ResponseModel.UserGeneralResponse;
import red.stevo.code.masenomedlabclub.Models.ResponseModel.UserResponse;
import red.stevo.code.masenomedlabclub.Service.AdminIndexImagesStorageService;
import red.stevo.code.masenomedlabclub.Service.UsersRegistrationService;
import red.stevo.code.masenomedlabclub.Service.events.EventImagesService;
import red.stevo.code.masenomedlabclub.Service.events.EventsService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/apis/admin")
@CrossOrigin(value={"http://localhost:5173"}, allowCredentials = "true")
public class AdminController {

    private final AdminIndexImagesStorageService adminIndexImagesStorageService;
    private final UsersRegistrationService usersRegistrationService;
    private final EventsService eventsService;
    private final EventImagesService imagesService;

    /*This end point handles storing of url, name and description of the upload images.
    * These values are received from the font-end provided by the cloudinary API.*/
    @PostMapping("/save/images")
    public ResponseEntity<UserGeneralResponse> uploadedImage(@RequestBody @Validated
                                                                 List<IndexPageImageModel> uploadedImage){
        log.info("Request to store upload images");
        return adminIndexImagesStorageService.storeUploadedImagesUrl(uploadedImage);
    }

    /*This API will handle fetching index page saved image urls,
    * description, image titles and their ids.
    * Returns a list of all available images in the table.*/
    @GetMapping("/get-all/images")
    public ResponseEntity<List<IndexPageImageModel>> getAllSavedImages(){
        log.info("Getting Index page Images.");
        return adminIndexImagesStorageService.getAllIndexPageImages();
    }

    @DeleteMapping("/delete/image/{image-publicId}")
    public ResponseEntity<UserGeneralResponse> deleteIndexPAgeImage(@PathVariable ("image-publicId")String imageId){
        log.info("Request to delete index image.");
        return adminIndexImagesStorageService.deleteIndexPageImage(imageId);
    }



    @DeleteMapping("/delete")
    public ResponseEntity<UserGeneralResponse> deleteUser(@RequestBody List<String> emails){
        log.info("Request to delete user.");
        try {
            UserGeneralResponse generalResponse = new UserGeneralResponse();
            usersRegistrationService.deleteUser(emails);
            return ResponseEntity.ok(generalResponse);
        }catch (Exception e){
            throw new EntityDeletionException("could not delete the user");
        }
    }

    @PostMapping("/events/create")
    public ResponseEntity<UserGeneralResponse> createEvent(@RequestBody EventsCreationRequest request){
        log.info("Request to create event.");
        UserGeneralResponse response = eventsService.createEvent(request);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/events/update/{eventId}")
    public ResponseEntity<UserGeneralResponse> updateEvent(@RequestBody EventsCreationRequest request,
                                              @PathVariable("eventId") String eventId){
        log.info("request to update an event");
        System.out.println(request.toString());
        UserGeneralResponse response =  eventsService.updateEvent(request,eventId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/event/delete")
    public ResponseEntity<String> deleteEvent(String eventId){
        eventsService.deleteEvent(eventId);
        return ResponseEntity.ok("event deleted successfully");
    }


    @DeleteMapping("/delete/image")
    public ResponseEntity<UserGeneralResponse> deleteEventImage(@RequestBody List<String> imageUrl) {
        UserGeneralResponse response = imagesService.deleteEventImages(imageUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserGeneralResponse> register(@RequestBody List<UsersRegistrationRequests> request){
        log.info("Request to register users.");
        UserGeneralResponse createUsers = usersRegistrationService.createUser(request);
        return ResponseEntity.ok(createUsers);
    }

    @GetMapping("/get_all_users")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        log.info("getting all the users");
        List<UserResponse> users = usersRegistrationService.getAllUsers();
        log.info("returning users to client");
        return ResponseEntity.ok(users);
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<UserGeneralResponse> updateUser(@RequestBody UserResponse regRequest,
                                                          @PathVariable int userId){
        UserGeneralResponse response = usersRegistrationService.updateUser(regRequest, userId);
        return ResponseEntity.ok(response);
    }
}
