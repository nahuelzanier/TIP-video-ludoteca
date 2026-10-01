package com.tip_video_ludoteca.config;

import com.tip_video_ludoteca.comments.CommentNotFoundException;
import com.tip_video_ludoteca.comments.InvalidCommentException;
import com.tip_video_ludoteca.games.GameEditForbiddenException;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.users.ProfileEditForbiddenException;
import com.tip_video_ludoteca.users.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> userNotFound(
            UserNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, String>> gameNotFound(
            GameNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Map<String, String>> commentNotFound(
            CommentNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidCommentException.class)
    public ResponseEntity<Map<String, String>> invalidComment(
            InvalidCommentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ProfileEditForbiddenException.class)
    public ResponseEntity<Map<String, String>> profileEditForbidden(
            ProfileEditForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(GameEditForbiddenException.class)
    public ResponseEntity<Map<String, String>> gameEditForbidden(
            GameEditForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidBody(
            MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();

        String message = fieldError == null
                ? "La solicitud no es válida."
                : fieldError.getDefaultMessage();

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
