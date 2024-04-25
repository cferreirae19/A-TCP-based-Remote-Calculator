import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class tcp1cli {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Numero incorrecto de argumentos. Sintaxis correcta: java tcp1cli direccion_ip_servidor numero_puerto_servidor");
            return;
        }

        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverAddress, port), 15000);
            socket.setSoTimeout(15000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.print("Introduce una operacion o QUIT para finalizar (Ejemplos de operaciones validas: 8+5, 8*5, 8!, etc.): ");
                String userInputLine = userInput.readLine();
                if (userInputLine == null || userInputLine.equalsIgnoreCase("QUIT")) {
                    break;
                }

                // We encode the input as a byte array and we send that message to the server
                byte[] encodedClientMessage = encodeClientMessage(userInputLine);
                output.write(encodedClientMessage);
                // Ensure that the data is sent inmediately
                output.flush();

                /*TESTING======================================
                System.out.println("Client message sent: " + Arrays.toString(encodedClientMessage));
                =============================================*/

                long response = decodeServerMessage(socket.getInputStream());
                System.out.println("Respuesta del servidor (valor del acumulador): " + response);
            }

            socket.close();
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout alcanzado (15s)");
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    public static byte[] encodeClientMessage(String s) {
        // Split the string by operators
        String[] parts = s.split("[\\+\\-\\*\\/\\%\\!]");

        // Remove empty strings resulting from splitting
        parts = Arrays.stream(parts).filter(part -> !part.isEmpty()).toArray(String[]::new);

        // Parse the numbers
        int number1 = Integer.parseInt(parts[0]);
        int number2 = (parts.length == 2) ? Integer.parseInt(parts[1]) : 0;

        // Determine op_code and length based on the operation
        byte op_code;
        byte length;

        if (s.contains("+")) {
            op_code = 1;
            length = 2;
        } else if (s.contains("-")) {
            op_code = 2;
            length = 2;
        } else if (s.contains("*")) {
            op_code = 3;
            length = 2;
        } else if (s.contains("/")) {
            op_code = 4;
            length = 2;
        } else if (s.contains("%")) {
            op_code = 5;
            length = 2;
        } else { // Assuming factorial operation
            op_code = 6;
            length = 1;
        }

        // Build the message array
        byte[] message = new byte[length + 2];
        message[0] = op_code;
        message[1] = length;
        message[2] = (byte) number1;
        if (length == 2) {
            message[3] = (byte) number2;
        }

        return message;
    }

    public static long decodeServerMessage(InputStream inputStream) throws IOException {
        
        byte[] message = new byte[10];
        inputStream.read(message);

        /*TESTING======================================
        System.out.println("Server message decoded: " + Arrays.toString(message));
        =============================================*/
        
        // Check if message has correct length
        if (message.length != 10) {
            throw new IllegalArgumentException("Invalid byte count");
        }
        
        // Check op_code (should be 16)
        if (message[0] != 16) {
            throw new IllegalArgumentException("Invalid op_code");
        }

        // Check length (should be 8)
        if (message[1] != 8) {
            throw new IllegalArgumentException("Invalid length");
        }

        // Extract accumulator bytes from message
        byte[] accumulatorBytes = new byte[8];
        System.arraycopy(message, 2, accumulatorBytes, 0, 8);

        // Convert accumulator bytes to long (64-bit signed integer in big-endian format)
        return ByteBuffer.wrap(accumulatorBytes).getLong();
    }

}