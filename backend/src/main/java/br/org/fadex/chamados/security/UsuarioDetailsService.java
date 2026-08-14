package br.org.fadex.chamados.security;

import br.org.fadex.chamados.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carrega o usuario pelo e-mail, que e o identificador de login do sistema. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository
                .findByEmailIgnoreCase(email)
                .map(UsuarioAutenticado::new)
                // Mensagem propositalmente generica: nao revela se o e-mail existe.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }
}
