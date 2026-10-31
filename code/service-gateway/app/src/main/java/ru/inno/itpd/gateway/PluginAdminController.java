package ru.inno.itpd.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.inno.itpd.gateway.PluginDtos.ErrorResponse;
import ru.inno.itpd.gateway.PluginDtos.PluginInfo;
import ru.inno.itpd.gateway.PluginDtos.Settings;
import ru.inno.itpd.gateway.PluginDtos.Status;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/plugins")
@RequiredArgsConstructor
@Tag(name = "Plugin administration",
        description = "Manage plugins: upload, settings, enabling, ordering and deletion")
@SecurityRequirement(name = "HTTPBearer")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Malformed request, invalid plugin name or ordering hint.",
                headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid admin bearer credentials.",
                headers = {
                        @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                        @Header(name = "WWW-Authenticate", description = "Bearer authentication is required.", schema = @Schema(type = "string"))},
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Plugin storage operation failed.",
                headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Plugin administration is disabled because GATEWAY_ADMIN_API_KEY is unset.",
                headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
})
public class PluginAdminController {
    private final PluginService service;
    private final PythonValidationService pythonValidator;

    @GetMapping
    @Operation(summary = "List Plugins", description = "Returns all plugins in chain order.")
    @ApiResponse(responseCode = "200", description = "Successful Response")
    public List<PluginInfo> list() {
        return service.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload Plugin",
            description = "Uploads a Python plugin file. The plugin name is the filename without the .py extension. "
                    + "A new plugin is created disabled; without before/after it is appended to the end of the chain.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Successful Response"),
            @ApiResponse(responseCode = "400",
                    description = "Malformed multipart request, invalid filename or Python source, invalid ordering hint, invalid plugin type, or options that are not a JSON object.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ordering target has no configuration entry.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Plugin already exists.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Plugin source exceeds the 1 MiB limit.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Options cannot be represented in TOML, for example JSON null.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PluginInfo upload(
            @Parameter(description = "Plugin source file named <name>.py, up to 1 MiB") @RequestParam MultipartFile file,
            @Parameter(description = "Plugin type; cannot be changed after creation",
                    schema = @Schema(allowableValues = {"request_filter", "response_filter"}))
            @RequestParam String type,
            @Parameter(description = "Plugin options as a JSON object", example = "{}") @RequestParam(defaultValue = "{}") String options,
            @Parameter(description = "Insert immediately before this configured plugin; mutually exclusive with after.")
            @RequestParam(required = false) String before,
            @Parameter(description = "Insert immediately after this configured plugin; mutually exclusive with before.")
            @RequestParam(required = false) String after) throws IOException {
        String name = PluginUtils.nameFromFilename(file.getOriginalFilename());
        PluginType pluginType = PluginUtils.parseType(type);
        var parsedOptions = PluginUtils.parseOptions(options);
        byte[] source = file.getBytes();
        pythonValidator.validate(source);
        return service.create(name, pluginType, parsedOptions, before, after, source);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get Plugin", description = "Returns a plugin by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful Response"),
            @ApiResponse(responseCode = "404", description = "Plugin not found.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PluginInfo get(@Parameter(description = "Plugin name") @PathVariable String name) {
        return service.get(name);
    }

    @PutMapping("/{name}")
    @Operation(summary = "Update Settings",
            description = "Replaces the plugin options entirely; optionally moves the plugin within the chain.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful Response"),
            @ApiResponse(responseCode = "404", description = "Plugin not found, or ordering target has no configuration entry.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Options cannot be represented in TOML, for example JSON null.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PluginInfo update(@Parameter(description = "Plugin name") @PathVariable String name,
                             @RequestBody Settings body) {
        var options = PluginUtils.checkOptions(body.options());
        return service.updateSettings(name, options, body.before(), body.after());
    }

    @PatchMapping("/{name}")
    @Operation(summary = "Set Status",
            description = "Enables or disables the plugin; optionally moves it within the chain.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful Response"),
            @ApiResponse(responseCode = "404", description = "Plugin not found, or ordering target has no configuration entry.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PluginInfo patch(@Parameter(description = "Plugin name") @PathVariable String name,
                            @RequestBody Status body) {
        return service.setStatus(name, body.enabled(), body.before(), body.after());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete Plugin", description = "Deletes a disabled plugin together with its source file.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successful Response"),
            @ApiResponse(responseCode = "404", description = "Plugin not found.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Plugin is enabled.",
                    headers = @Header(name = "X-Request-ID", description = "Identifier for this request.", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@Parameter(description = "Plugin name") @PathVariable String name) {
        service.delete(name);
    }
}