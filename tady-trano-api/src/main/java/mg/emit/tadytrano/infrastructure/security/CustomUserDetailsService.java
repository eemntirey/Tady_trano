package mg.emit.tadytrano.infrastructure.security;

import lombok.RequiredArgsConstructor;
import mg.emit.tadytrano.domain.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String emailNormalise = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        return utilisateurRepository.findByEmailIgnoreCase(emailNormalise)
                .map(u -> User.builder()
                        .username(u.getEmail())
                        .password(u.getMotDePasse())
                        .authorities("ROLE_" + u.getRole().name())
                        .disabled(!u.isActif())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }
}
