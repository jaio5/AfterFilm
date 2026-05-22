package alicanteweb.pelisapp.service;

/**
 * Interfaz para servicios de email
 * Permite usar ResendEmailService o MockEmailService según configuración
 */
public interface IEmailService {

    /**
     * Envía email de confirmación para registro de usuario
     * @param toEmail Email de destino
     * @param username Nombre de usuario
     * @param confirmationToken Token de confirmación único
     */
    void sendConfirmationEmail(String toEmail, String username, String confirmationToken);
}
