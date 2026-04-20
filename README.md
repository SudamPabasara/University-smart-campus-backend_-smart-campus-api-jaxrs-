# "Smart Campus" Sensor & Room Management API

## Overview
[cite_start]This repository contains the backend RESTful API for the university's "Smart Campus" initiative[cite: 24, 25, 26]. [cite_start]It is a high-performance web service built using JAX-RS (Jakarta RESTful Web Services) to manage campus rooms and diverse sensors (e.g., CO2 monitors, occupancy trackers, smart lighting controllers)[cite: 34, 35]. 

**Technology Stack:**
* [cite_start]**Framework:** JAX-RS with a lightweight servlet container/embedded server[cite: 103].
* [cite_start]**Data Storage:** In-memory data structures (e.g., HashMaps, ArrayLists) are used strictly as per module constraints[cite: 207]. [cite_start]No external database technologies (like SQL Server) or alternative frameworks (like Spring Boot) are utilized[cite: 204, 206].

---

## Build and Launch Instructions
*(Student Note: Update these instructions based on your specific IDE or build tool. Below is a standard Maven example)*

1. **Clone the repository:**
   `git clone <your-repository-url>`
2. **Navigate to the project directory:**
   `cd <your-repository-name>`
3. **Build the project using Maven:**
   `mvn clean install`
4. **Run the server:**
   `mvn exec:java` 
5. **Access the API:** The application will start locally. [cite_start]The versioned entry point for the API is `http://localhost:<port>/api/v1`[cite: 104].

---

## Sample API Interactions (cURL Commands)
[cite_start]Below are five sample commands demonstrating successful interactions with the Smart Campus API[cite: 176]:

**1. Discovery Endpoint:**
`curl -X GET http://localhost:8080/api/v1`

**2. Get All Rooms:**
`curl -X GET http://localhost:8080/api/v1/rooms`

**3. Create a New Room:**
`curl -X POST http://localhost:8080/api/v1/rooms \`
`-H "Content-Type: application/json" \`
`-d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 50}'`

**4. Register a New Sensor to a Room:**
`curl -X POST http://localhost:8080/api/v1/sensors \`
`-H "Content-Type: application/json" \`
`-d '{"id": "TEMP-001", "type": "Temperature", "status": "ACTIVE", "roomId": "LIB-301"}'`

**5. Retrieve Sensors by Type (Filtered):**
`curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"`

---

## Conceptual Report

**Part 1: Service Architecture & Setup**

* **Question 1.1: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? [cite_start]Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.** [cite: 105, 106, 107]
    * **Answer:** By default, the JAX-RS runtime instantiates a new instance of a Resource class for every incoming HTTP request (per-request lifecycle). Because a new object handles each request, standard instance variables (like a regular `HashMap`) will not persist data across multiple client calls. To maintain state across requests using in-memory structures, these maps or lists must either be declared as `static`, or the resource class must be explicitly configured as a `@Singleton`. Furthermore, because a web server handles multiple requests concurrently using different threads, these shared data structures are highly vulnerable to race conditions. To prevent data corruption, developers must use thread-safe collections (such as `ConcurrentHashMap` or `CopyOnWriteArrayList`) or carefully implement `synchronized` blocks when reading and writing data.

* **Question 1.2: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? [cite_start]How does this approach benefit client developers compared to static documentation?** [cite: 110, 111]
    * **Answer:** HATEOAS (Hypermedia As The Engine Of Application State) is an advanced REST principle where the server embeds navigational links in its JSON responses, explicitly telling the client what state transitions or actions are currently possible. This benefits client developers because it decouples the client from hardcoded API URLs. Instead of relying heavily on static documentation to figure out endpoint structures, the client simply follows the URLs provided dynamically by the server. If the backend URL structure changes in the future, the client naturally adapts without breaking, as it relies on the provided hypermedia links rather than static, hardcoded paths.

**Part 2: Room Management**

* **Question 2.1: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? [cite_start]Consider network bandwidth and client-side processing.** [cite: 118, 119]
    * **Answer:** Returning only IDs drastically reduces the payload size, saving network bandwidth and speeding up the initial transfer. However, if the client needs to display details for those rooms (like names or capacities), it forces the client to make subsequent, individual API calls for each ID (the "N+1 problem"). This significantly increases overall latency and server load. Conversely, returning the full room objects increases the initial network payload and server serialization time, but it provides the client with all necessary information in a single round-trip, resulting in a faster, smoother rendering experience on the front-end.

* **Question 2.2: Is the DELETE operation idempotent in your implementation? [cite_start]Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.** [cite: 124]
    * **Answer:** Yes, the DELETE operation is idempotent. Idempotency dictates that performing an operation multiple times yields the same final server state as performing it once. If a client sends a DELETE request for a room, the first request will successfully remove the room and return a `200 OK` or `204 No Content`. If the client mistakenly sends the exact same DELETE request again, the server will not crash or alter any other data; it will simply recognize that the room does not exist and return a `404 Not Found`. Regardless of how many times the request is sent, the end result on the server's state remains exactly the same: the target room is absent from the system.

**Part 3: Sensor Operations & Linking**

* **Question 3.1: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. [cite_start]How does JAX-RS handle this mismatch?** [cite: 130, 131, 132]
    * **Answer:** The `@Consumes` annotation acts as a strict contract for the incoming payload's content type. If a client attempts to send data using an unsupported format like `text/plain` or `application/xml`, the JAX-RS framework intercepts the request before it even reaches the Java method. JAX-RS automatically handles this mismatch by rejecting the request and returning an HTTP `415 Unsupported Media Type` response to the client. This protects the backend logic from attempting to parse incompatible data formats and throwing internal serialization exceptions.

* **Question 3.2: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). [cite_start]Why is the query parameter approach generally considered superior for filtering and searching collections?** [cite: 136, 137]
    * **Answer:** Path parameters (`@PathParam`) are best suited for identifying specific resources or establishing a strict resource hierarchy (e.g., identifying a specific sensor by its ID). Query parameters (`@QueryParam`) are designed for modifying, filtering, or sorting a collection without fundamentally changing what the resource is. The query parameter approach is superior for filtering because query strings are inherently optional and highly composable. A client can easily combine multiple filters (e.g., `?type=CO2&status=ACTIVE`) without the server needing to define complex, rigid route permutations. A path-based approach for filtering quickly leads to brittle and difficult-to-maintain URL structures as search criteria grow.

**Part 4: Deep Nesting with Sub-Resources**

* **Question 4.1: Discuss the architectural benefits of the Sub-Resource Locator pattern. [cite_start]How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?** [cite: 142, 143]
    * **Answer:** The Sub-Resource Locator pattern prevents bloated, monolithic controller classes by returning instances of other resource classes rather than handling the request directly. In a large API, defining every deeply nested route inside `SensorResource` violates the Single Responsibility Principle. By using a locator (e.g., `@Path("{id}/readings")` returning a `SensorReadingResource`), the system encapsulates all reading-specific logic, HTTP methods, and dependencies into its own distinct class. This delegation makes the codebase far more modular, easier to test, simpler for multiple developers to work on simultaneously, and significantly easier to maintain as the API hierarchy scales.

**Part 5: Advanced Error Handling, Exception Mapping & Logging**

* [cite_start]**Question 5.2: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?** [cite: 157]
    * **Answer:** An HTTP `404 Not Found` implies that the target URI/endpoint itself does not exist. If a client successfully reaches the `POST /api/v1/sensors` endpoint, returning a 404 is confusing because the endpoint *is* valid. An HTTP `422 Unprocessable Entity` is much more semantically accurate. It informs the client that the server understands the content type of the request, and the JSON syntax is perfectly valid, but the server was unable to process the contained instructions due to semantic errors (in this case, referencing a `roomId` that does not exist in the database).

* **Question 5.4: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. [cite_start]What specific information could an attacker gather from such a trace?** [cite: 163, 164]
    * **Answer:** Exposing raw Java stack traces acts as an unintentional information leak. From a cybersecurity perspective, this is dangerous because it gives malicious actors a detailed blueprint of the server's internal architecture. An attacker can gather highly specific information, including the exact names and versions of frameworks being used (e.g., Jersey, Jackson, Hibernate), internal package and class naming conventions, underlying database driver details, and even file paths on the server. Hackers can cross-reference this specific framework and version information against databases of known vulnerabilities (CVEs) to launch targeted exploits.

* [cite_start]**Question 5.5: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?** [cite: 168]
    * **Answer:** Using JAX-RS filters (`ContainerRequestFilter`, `ContainerResponseFilter`) adheres to the DRY (Don't Repeat Yourself) principle and promotes separation of concerns. Manually inserting `Logger.info()` inside every method clutters business logic with infrastructural code and introduces the risk of human error (e.g., a developer forgets to log a newly created endpoint). Filters provide centralized, global interception. They guarantee that every incoming request and outgoing response is uniformly logged—capturing metadata like URIs, methods, and status codes—completely independently of the individual resource methods.
