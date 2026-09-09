# Hotel API standard

This contract is mandatory for every backend endpoint. The architecture test
fails the build when a controller does not follow the structural rules.

## Controller skeleton

```java
@ApiController
@RequestMapping(ApiPaths.V1 + "/resources")
@RequiredArgsConstructor
public class ResourceApi {
    private final ResourceService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> get(@PathVariable Integer id) {
        return ApiResponses.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> create(
            @Valid @RequestBody CreateResourceRequest request) {
        ResourceResponse created = service.create(request);
        return ApiResponses.created(created.id(), created);
    }
}
```

## Mandatory rules

1. Use `@ApiController`; do not add plain `@RestController` APIs.
2. Root business endpoints at `ApiPaths.V1`. Use plural resource names and
   nouns. Keep old action routes only for backward compatibility.
3. Return `ResponseEntity<ApiResponse<T>>`. Only file/stream downloads bypass
   the envelope.
4. Request and response types live in the feature `dto` package. Never return
   a JPA `@Entity`, repository result, password, token, identity document, or
   internal exception message.
5. Validate every external input with Bean Validation and `@Valid`. Put domain
   rules in the service transaction, not in the controller.
6. Map entities to immutable response DTOs while the transaction is open.
   Cache response DTOs/read models, never managed entities or lazy proxies.
7. Use `ApiException` subclasses and a stable constant from `ErrorCodes`.
   Do not build ad-hoc error JSON or switch frontend logic on message text.
8. Public routes must be explicitly allowlisted in `SecurityConfig`. Everything
   else under `/api/v1/**` is authenticated by default. Admin APIs require both
   the security-chain rule and `@PreAuthorize("hasRole('ADMIN')")`.
9. State-changing browser requests require the CSRF header. Webhooks require a
   provider signature and replay protection. Never put secrets in source code.
10. Collection endpoints that can grow must use `PageResponse<T>` and enforce a
    bounded page size. Do not expose Spring Data `Page` JSON directly.

## Wire format

Success:

```json
{
  "data": {},
  "meta": {
    "timestamp": "2026-09-09T10:00:00Z",
    "apiVersion": "v1",
    "requestId": "01J..."
  }
}
```

Failure:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed",
    "details": ["email: must be a well-formed email address"]
  },
  "meta": {
    "timestamp": "2026-09-09T10:00:00Z",
    "apiVersion": "v1",
    "requestId": "01J..."
  }
}
```

The `requestId` is the support correlation key. Logs may contain diagnostics;
responses must contain only client-safe information.
