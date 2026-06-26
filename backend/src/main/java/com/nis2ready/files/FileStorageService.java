package com.nis2ready.files;

import com.nis2ready.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
  private static final Set<String> EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "docx", "xlsx", "csv", "txt");
  private static final Map<String, String> CONTENT_TYPES = Map.of(
    "pdf", "application/pdf",
    "png", "image/png",
    "jpg", "image/jpeg",
    "jpeg", "image/jpeg",
    "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "csv", "text/csv",
    "txt", "text/plain"
  );
  private final Path root;
  private final StoredFileRepository files;
  private final long maxSizeBytes;
  public FileStorageService(@Value("${app.evidence-storage-path}") String root,
                            @Value("${app.max-evidence-file-size-bytes:10485760}") long maxSizeBytes,
                            StoredFileRepository files) {
    this.root = Path.of(root).toAbsolutePath().normalize();
    this.maxSizeBytes = maxSizeBytes;
    this.files = files;
  }
  public StoredFile store(UUID orgId, UUID userId, MultipartFile file) {
    try {
      if (file.isEmpty()) throw ApiException.badRequest("Empty file");
      if (file.getSize() > maxSizeBytes) throw ApiException.badRequest("file too large");
      String original = safeOriginalFilename(file.getOriginalFilename());
      String extension = extension(original);
      if (!EXTENSIONS.contains(extension)) throw ApiException.badRequest("unsupported file type");
      byte[] bytes = file.getBytes();
      validateContent(extension, bytes);
      Files.createDirectories(root.resolve(orgId.toString()));
      String storedName = UUID.randomUUID() + "." + extension;
      Path target = root.resolve(orgId.toString()).resolve(storedName).normalize();
      if (!target.startsWith(root.resolve(orgId.toString()))) throw ApiException.badRequest("Invalid file path");
      Files.write(target, bytes);
      var stored = new StoredFile();
      stored.organizationId = orgId; stored.originalFilename = original; stored.storedFilename = storedName;
      stored.storagePath = orgId + "/" + storedName;
      stored.contentType = CONTENT_TYPES.get(extension);
      stored.sizeBytes = bytes.length; stored.checksumSha256 = sha256(bytes); stored.uploadedBy = userId;
      return files.save(stored);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw ApiException.badRequest("file upload error: " + e.getMessage());
    }
  }
  public Resource load(UUID orgId, StoredFile file) {
    try {
      Path path = root.resolve(orgId.toString()).resolve(file.storedFilename).normalize();
      if (!path.startsWith(root.resolve(orgId.toString()))) throw ApiException.badRequest("Invalid file path");
      return new UrlResource(path.toUri());
    } catch (Exception e) {
      throw ApiException.notFound("File not found");
    }
  }
  public void delete(UUID orgId, StoredFile file) {
    try {
      Path path = root.resolve(orgId.toString()).resolve(file.storedFilename).normalize();
      if (!path.startsWith(root.resolve(orgId.toString()))) throw ApiException.badRequest("Invalid file path");
      Files.deleteIfExists(path);
      files.delete(file);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw ApiException.badRequest("file delete error: " + e.getMessage());
    }
  }
  private String extension(String filename) {
    int i = filename.lastIndexOf('.');
    return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
  }
  private String safeOriginalFilename(String filename) {
    String name = filename == null ? "evidence" : filename.replace('\\', '/');
    int slash = name.lastIndexOf('/');
    String base = slash >= 0 ? name.substring(slash + 1) : name;
    base = base.replaceAll("[/\\\\\\r\\n\\t\\x00-\\x1F\\x7F\"]", "_").trim();
    if (base.isBlank()) base = "evidence";
    return base.length() > 180 ? base.substring(0, 180) : base;
  }
  private String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
  private void validateContent(String extension, byte[] bytes) {
    if (bytes.length == 0) throw ApiException.badRequest("Empty file");
    boolean ok = switch (extension) {
      case "pdf" -> startsWith(bytes, "%PDF".getBytes());
      case "png" -> bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
      case "jpg", "jpeg" -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
      case "docx", "xlsx" -> startsWith(bytes, new byte[] {0x50, 0x4b});
      case "csv", "txt" -> Arrays.stream(toUnsigned(bytes, Math.min(bytes.length, 2048))).noneMatch(b -> b == 0);
      default -> false;
    };
    if (!ok) throw ApiException.badRequest("file content does not match supported file type");
  }
  private boolean startsWith(byte[] bytes, byte[] prefix) {
    if (bytes.length < prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) if (bytes[i] != prefix[i]) return false;
    return true;
  }
  private int[] toUnsigned(byte[] bytes, int length) {
    int[] values = new int[length];
    for (int i = 0; i < length; i++) values[i] = bytes[i] & 0xff;
    return values;
  }
}
