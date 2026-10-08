# Código fuente de NanoFiles

El código separa el arranque de las aplicaciones, la interacción con el usuario y los mecanismos de comunicación.

| Paquete | Responsabilidad |
|---|---|
| `application` | Puntos de entrada del directorio y del nodo |
| `logic` | Coordinación de las operaciones de la aplicación |
| `shell` | Interacción desde la consola |
| `udp` | Comunicación con el directorio mediante datagramas |
| `tcp` | Comunicación entre nodos y transferencia de archivos |
| `util` | Utilidades compartidas |

## Flujo general

El nodo recibe una operación desde la consola, utiliza el directorio para localizar información de los pares y establece conexiones TCP cuando necesita transferir contenido. El directorio y los pares tienen responsabilidades distintas dentro del protocolo.

Para estudiar el diseño, empieza por las clases de `application` y sigue las llamadas hacia los componentes de lógica y comunicación.
