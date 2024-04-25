import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class tcp1ser {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Numero incorrecto de argumentos. Sintaxis correcta: java tcp1ser num_puerto");
            return;
        }

        int port = Integer.parseInt(args[0]);
        // The accumulator must be a 64-bit signed integer
        long accumulator = 0;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server iniciado en el puerto " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente ha iniciado conexion desde " + clientSocket.getInetAddress());

                try (
                    DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())
                ) {
                    byte[] receivedClientMessageUnfiltered = new byte[4];

                    while (true) {
                        try {
                            if (input.read(receivedClientMessageUnfiltered) == -1) {
                                // If read returns -1, the client has disconnected
                                System.out.println("Cliente se ha desconectado");
                                break; // Break out of the inner loop and wait for a new connection
                            }

                            byte[] receivedClientMessage;

                            if (receivedClientMessageUnfiltered[0] == 6) {
                                receivedClientMessage = new byte[3];
                                receivedClientMessage[0] = receivedClientMessageUnfiltered[0];
                                receivedClientMessage[1] = receivedClientMessageUnfiltered[1];
                                receivedClientMessage[2] = receivedClientMessageUnfiltered[2];
                            } else {
                                receivedClientMessage = new byte[4];
                                receivedClientMessage = receivedClientMessageUnfiltered;
                            }

                            /*TESTING======================================
                            System.out.println("Client message received: " + Arrays.toString(receivedClientMessage));
                            =============================================*/

                            // Decode the client message (i.e., evaluate the operation), store the result in the number variable and add it to the accumulator
                            long number = decodeClientMessage(receivedClientMessage);
                            accumulator += number;
                            System.out.println("Resultado: " + number);
                            System.out.println("Valor actual del acumulador: " + accumulator);

                            byte[] encodedServerMessage = encodeServerMessage(accumulator);

                            /*TESTING======================================
                            int[] maskedEncodedServerMessage = new int[10];
                            for (int i=0; i<10; i++) {
                                maskedEncodedServerMessage[i] = (int) (encodedServerMessage[i]&0xff);
                            }
                            System.out.println("Server message sent: " + Arrays.toString(encodedServerMessage) + " /// Masked server message: " + Arrays.toString(maskedEncodedServerMessage));
                            =============================================*/

                            // Send the response back to the client immediately after processing the message
                            output.write(encodedServerMessage);
                            output.flush();
                        } catch (IOException e) {
                            if (e.getMessage().equals("Connection reset")) {
                                // Handle connection reset specifically
                                System.out.println("Cliente se ha desconectado");
                                break; // Break out of the inner loop and wait for a new connection
                            } else {
                                // Print other IOException errors
                                System.err.println("I/O error: " + e.getMessage());
                                break; // Break out of the inner loop and wait for a new connection
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error: No valid number");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    // Method to evaluate the operation based on the received (and encoded) message from the client
    public static long decodeClientMessage(byte[] message) {
        int op_code = message[0];
        int length = message[1];
        int number1 = message[2];
        int number2 = (length == 2) ? message[3] : 0;

        switch (op_code) {
            case 1: // Sum
                System.out.println("Operacion recibida: " + number1 + "+" + number2);
                return number1 + number2;
            case 2: // Subtraction
                System.out.println("Operacion recibida: " + number1 + "-" + number2);
                return number1 - number2;
            case 3: // Multiplication
                System.out.println("Operacion recibida: " + number1 + "*" + number2);
                return number1 * number2;
            case 4: // Division
                System.out.println("Operacion recibida: " + number1 + "/" + number2);
                return number1 / number2;
            case 5: // Modulus
                System.out.println("Operacion recibida: " + number1 + "%" + number2);
                return number1 % number2;
            case 6: // Factorial
                System.out.println("Operacion recibida: " + number1 + "!");
                return factorial(number1);
            default:
                throw new IllegalArgumentException("Invalid operation code");
        }
    }

    // Helper method to compute factorial
    private static long factorial(int n) {
        if (n == 0) {
            return 1;
        }
        long result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public static byte[] encodeServerMessage(long accumulator) {
        byte[] message = new byte[10];

        // Now the op_code is always 16
        message[0] = 16;

        // Now the length is always 8
        message[1] = 8;

        // Convert accumulator to byte array (64-bit signed integer in big-endian format)
        byte[] accumulatorBytes = ByteBuffer.allocate(8).putLong(accumulator).array();

        // Copy accumulator bytes to the message array
        System.arraycopy(accumulatorBytes, 0, message, 2, 8);

        return message;
    }
}
