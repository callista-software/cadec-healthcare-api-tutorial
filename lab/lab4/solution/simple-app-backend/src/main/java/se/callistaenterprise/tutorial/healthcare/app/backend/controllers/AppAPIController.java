package se.callistaenterprise.tutorial.healthcare.app.backend.controllers;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import se.callistaenterprise.tutorial.healthcare.app.backend.repositories.UserAccessTokenRepository;
import se.callistaenterprise.tutorial.healthcare.app.backend.types.Booking;
import se.callistaenterprise.tutorial.healthcare.app.backend.types.Bookings;
import se.callistaenterprise.tutorial.healthcare.app.backend.types.UserAccessToken;

@Controller
@RequestMapping("/api")
public class AppAPIController {

	private static final String API_SERVER_URL = "http://dev.apigw.minavardkontakter.se";
	
	@Autowired
	private UserAccessTokenRepository userAccessTokenRepository;
	
	/**
	 * This controller method fetches the access token of the logged in user, retrieves a 
	 * schedule for that user and returns it as application/json.
	 * The ResponseBody annotation makes Spring MVC use Jackson to transform the returned
	 * POJOS to JSON.
	 * @return A response consisting of a list of booking POJOs to be transformed to JSON.
	 */
	@RequestMapping(method = RequestMethod.GET, value="/bookings", produces="application/json")  
	public @ResponseBody List<Booking> getBookings() {
		// Get the logged in user
		User user  = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// Find the stored access token for the logged in user
		UserAccessToken userAccessToken = userAccessTokenRepository.findByUser(user.getUsername());
		// Use the access token to fetch the bookings of the logged in user
		return fetchSchedule(userAccessToken.getAccessToken()).getBookings();
	}
	
	/**
	 * Use RestTemplate to fetch a patient schedule
	 * @param accessToken the access token for this patients scheduling information
	 * @return the schedule of this patient as json
	 */
	private Bookings fetchSchedule(String accessToken) {		
		RestTemplate scheduleEndpoint = new RestTemplate();
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken );
		HttpEntity<Void> requestEntity = new HttpEntity<Void>(headers);
		// RestTemplate uses Jackson to map the JSON response to POJOS with Jackson annotations. See Bookings.java
		ResponseEntity<Bookings> scheduleEnpointResponse = scheduleEndpoint.exchange(API_SERVER_URL + "/crm/scheduling/v1/schedule", 
				HttpMethod.GET, requestEntity, Bookings.class);
		return scheduleEnpointResponse.getBody();
	}
	
	
	/**
	 * Checks the login status and returns a json object with
	 * isAuthenticated, username and an array of roles.
	 * @return a json object representing the login status
	 */
    @RequestMapping(value = "/loginstatus", produces = "application/json")
    public @ResponseBody String loginStatus() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        String response = "{isAuthenticated: false}";
        if (authentication != null && authentication.isAuthenticated()) {
            response = "{\"isAuthenticated\" : true, " + "\"username\" : "
                    + "\"" + authentication.getName() + "\" , " + "\"roles\" : [";

            for (Iterator<? extends GrantedAuthority> it = authentication
                    .getAuthorities().iterator(); it.hasNext();) {
                GrantedAuthority role = it.next();
                response += "\"" + role.getAuthority() + "\"";
                if (it.hasNext()) {
                    response += ", ";
                }
            }
            response += "]}";
        }
        return response;
    }
	
}
