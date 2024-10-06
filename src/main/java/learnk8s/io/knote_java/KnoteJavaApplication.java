package learnk8s.io.knote_java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
//import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;


//import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@EnableConfigurationProperties(properties.class)
@SpringBootApplication
public class KnoteJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnoteJavaApplication.class, args);
    }

}

@Document(collection = "notes")
@Setter
@Getter
@NoArgsConstructor
//@AllArgsConstructor
class Note {
    @Id
    private String id;
    private String description;

    // Manually create this constructor if Lombok doesn't work as expected
    public Note(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}

interface NotesRepository extends MongoRepository<Note, String> {

}

@Controller
class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private properties properties;

    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    @PostMapping("/note")
    public String saveNotes(@RequestParam("image") MultipartFile file,
            @RequestParam String description,
            @RequestParam(required = false) String publish,
            @RequestParam(required = false) String upload,
            Model model) throws Exception {

        if (publish != null && publish.equals("Publish")) {
            saveNote(description, model);
            getAllNotes(model);
            return "redirect:/";
        }
        if (upload != null && upload.equals("Upload")) {
            if (file != null && file.getOriginalFilename() != null
                  && !file.getOriginalFilename().isEmpty()) {
              uploadImage(file, description, model);
            }
            getAllNotes(model);
            return "index";
          }
        // After save fetch all notes again
        return "index";
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {notesRepository.save(new Note(null, description.trim()));
          //After publish you need to clean up the textarea 
            model.addAttribute("description", "");
        }
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
        File uploadsDir = new File(properties.getUploadDir()); 
        if (!uploadsDir.exists()) {
          uploadsDir.mkdir();
        }
        String fileId = UUID.randomUUID().toString() + "."
                          + file.getOriginalFilename().split("\\.")[1];
        file.transferTo(new File(properties.getUploadDir() + fileId));
        model.addAttribute("description", description + " ![](/uploads/" + fileId + ")");
      }
}

@ConfigurationProperties(prefix = "knote")
class properties {
    @Value("${uploadDir:/tmp/uploads/}")
    private String uploadDir;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}

@Configuration
@EnableConfigurationProperties(properties.class)
class KnoteConfig implements WebMvcConfigurer {

    @Autowired
    private properties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + properties.getUploadDir())
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }

}