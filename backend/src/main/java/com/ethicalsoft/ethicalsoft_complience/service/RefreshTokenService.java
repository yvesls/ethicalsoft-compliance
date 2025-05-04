package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.model.RefreshToken;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.RefreshTokenDTO;
import com.ethicalsoft.ethicalsoft_complience.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	@Value( "${api.security.token.refresh.expiration}" )
	private Long refreshTokenDuration;

	public String createRefreshToken( User user ) {
		String token = UUID.randomUUID().toString();
		Instant expiryDate = Instant.now().plusMillis( refreshTokenDuration );
		String hashedToken = DigestUtils.sha256Hex(token);

		refreshTokenRepository.deleteByUser( user );

		refreshTokenRepository.save(new RefreshToken(hashedToken, user, expiryDate));
		return hashedToken;
	}

	public String validateRefreshToken( String token ) {
		String hashedToken = DigestUtils.sha256Hex(token);
		var refreshTokenOpt = refreshTokenRepository.findByTokenHash(hashedToken);
		if ( refreshTokenOpt.isEmpty() ) {
			throw new BusinessException( "Refresh token not found or invalid." );
		}
		RefreshToken refreshToken = refreshTokenOpt.get();
		if ( refreshToken.getExpiryDate().isBefore( Instant.now() ) ) {
			refreshTokenRepository.delete( refreshToken );
			throw new BusinessException( "Refresh token expired." );
		}
		return refreshToken.getUser().getEmail();
	}

	public User getUserFromRefreshToken( String token ) {
		return refreshTokenRepository.findByTokenHash( token ).map( RefreshToken::getUser ).orElseThrow( () -> new BusinessException( "Invalid refresh token" ) );
	}

	@Transactional
	public void deleteRefreshToken( RefreshTokenDTO refreshTokenDTO ) {
		refreshTokenRepository.deleteByTokenHash( refreshTokenDTO.getRefreshToken() );
	}
}