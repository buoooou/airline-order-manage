package com.postion.airlineorderbackend.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Function;

/**
 * Interface for JWT (JSON Web Token) operations.
 * Defines the contract for generating, validating, and extracting information from JWTs.
 */
public interface JwtService {

    /**
     * Extracts the username (subject) from a given JWT.
     *
     * @param token The JWT string.
     * @return The username contained in the token.
     */
    String extractUsername(String token);

    /**
     * Extracts a specific claim from the token using a provided claims resolver function.
     *
     * @param token          The JWT string.
     * @param claimsResolver A function that takes the claims and returns a specific value of type T.
     * @param <T>            The type of the claim to be returned.
     * @return The extracted claim.
     */
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    /**
     * Generates a new JWT for a given user.
     * The token will contain the username as the subject and an expiration date.
     *
     * @param userDetails The user details object representing the user.
     * @return A signed JWT string.
     */
    String generateToken(UserDetails userDetails);

    /**
     * Validates a JWT.
     * It checks if the token belongs to the given user and if it has not expired.
     *
     * @param token       The JWT string.
     * @param userDetails The user details to validate against.
     * @return true if the token is valid, false otherwise.
     */
    boolean isTokenValid(String token, UserDetails userDetails);
}