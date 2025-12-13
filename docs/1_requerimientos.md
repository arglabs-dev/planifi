# Requerimientos para Aplicación de Control de Gastos

## Funcionalidades Principales

1. **Gestión de Ingresos y Egresos**: Registro detallado de cada transacción, incluyendo:

   - Monto
   - Fecha
   - Etiquetas: selección libre de una o varias etiquetas sin jerarquía.
   - Descripción
   - Método de entrada: texto, voz, imagen
   - Archivos adjuntos: Permitir adjuntar archivos si el método de entrada ha sido voz o imagen, y también agregar más archivos si es necesario.

2. **Etiquetado de Gastos**: Sistema flexible basado en etiquetas (tags):
   - Sin categorías ni subcategorías jerárquicas.
   - Cada gasto puede tener múltiples etiquetas.
   - Las etiquetas se pueden crear dinámicamente en cualquier momento.

3. **Manejo de Múltiples Cuentas**: Administración de diferentes tipos de cuentas, tales como:

   - Tarjetas de crédito
   - Tarjetas de débito
   - Cuentas bancarias
   - Efectivo (cuenta cash)
   - Moneda: Capacidad de manejar cuentas en diferentes monedas como MXN, USD o Bitcoin.
   - Creación y eliminación de cuentas: El usuario puede crear, eliminar o deshabilitar cuentas según sus necesidades.
   - Tipos de cuenta: Cuentas con características específicas, como cuentas de débito, tarjetas de crédito, préstamos, deudas, o cuentas de nómina con ingresos programados. Esto ayudará a entender la capacidad de pago para planificar gastos futuros.

4. **Cargos Domiciliados**: Registro de cargos recurrentes para identificar y monitorear pagos automáticos, como servicios y suscripciones. Estos cargos deben tener la opción de:

   - Especificar la periodicidad y la fecha de cargo a las cuentas.
   - Confirmar con el usuario cada cargo (los movimientos se generan automáticamente pero el usuario debe aceptarlos a menos que esté en modo automático).
   - Finalizar el cargo según:
     - Una fecha específica.
     - Un número de pagos determinado.
     - Alcanzar un monto total previamente definido.

5. **Gestión Colaborativa de Cuentas**: Permitir que varias personas gestionen una misma cuenta, facilitando que usuarios (esposa, hijo, etc.) registren sus propios gastos.

6. **Importación Masiva de Datos**: Importación de datos mediante archivos Excel, CSV o JSON con la siguiente estructura específica:

   - **tipoMovimiento**: (ingreso o egreso)
   - **monto**: cantidad del movimiento
   - **fecha**: en formato ISO 8601 (e.g., "2024-11-25T15:30:00Z")
   - **etiquetas**: lista de etiquetas asociadas al gasto o ingreso (puede ser vacía y se podrá enriquecer después).
   - **descripción**: opcional
   - **cuenta**: identificador de la cuenta

7. **Registro Simplificado de Gastos e Ingresos**: El registro ocurre principalmente mediante interacción conversacional expuesta por el servidor MCP, y admite otros métodos simples como:

   - Mensajes de texto
   - Fotos de tickets
   - Mensajes de voz
   - Combinación de los métodos anteriores

   El backend procesará automáticamente estos registros para identificar la información clave (monto, fecha, etiquetas sugeridas) y confirmar la transacción con el usuario. Si el cliente conversacional (por ejemplo, un chat conectado al MCP) pierde conectividad con el backend, los movimientos quedan en estado pendiente y se sincronizan cuando vuelva la conexión; cualquier otra interfaz (como una app móvil futura) seguiría el mismo comportamiento opcional. Las entradas por imagen deben permitir extraer datos automáticamente y sugerir etiquetas usando capacidades multimodales del LLM.

8. **Backend-first con MCP como interfaz**:
   - El backend es el núcleo del sistema y expone operaciones para registrar gastos/ingresos y gestionar etiquetas.
   - No se depende de una app móvil en esta etapa; cualquier cliente es opcional y puede llegar después.
   - Un servidor **MCP (Model Context Protocol)** media entre el backend y los LLMs para exponer acciones estructuradas como crear etiquetas o registrar gastos.

9. **Interacción Conversacional**:
   - El usuario interactúa en lenguaje natural.
   - El LLM interpreta la intención y traduce la conversación en llamadas estructuradas al MCP.
   - El flujo conversacional es el medio principal para registrar movimientos, crear etiquetas y consultar información.

10. **Datos en Nube o Local**: El usuario puede definir si los datos se almacenarán solo localmente o si se subirán a la nube para acceder desde el portal web. Este punto puede ser opcional y se debe discutir más adelante durante el diseño.

11. **Backups en Nube o Local en el Dispositivo**:

   - **Frecuencia de Backups**: El usuario puede definir cada cuánto tiempo se realizarán los backups.
   - **Ubicación de los Backups**: Los backups se pueden guardar localmente o en la nube (e.g., Google Drive, Dropbox).
   - **Formato y Compresión**: Los backups deben estar en formato JSON y comprimidos para optimizar el espacio.
   - **Tipos de Backups**:
     - **Incrementales**: Los backups deben ser incrementales para ahorrar espacio.
     - **Completos**: El usuario puede generar un backup completo en cualquier momento y continuar desde allí.
   - **Archivo Descriptivo**: Cada backup debe tener un archivo descriptivo que incluya la fecha, si es incremental o completo, y detalles sobre su contenido.

12. **Alertas y Presupuestos**:

   - **Límites de Gasto por Etiqueta**: Posibilidad de definir límites de gasto por etiqueta y recibir alertas cuando se esté acercando a dichos límites.
   - **Planes de Presupuesto**:
     - El usuario puede definir un plan de presupuesto para una o varias etiquetas y hacer un seguimiento.
     - Cada presupuesto tiene un período definido: semanal, quincenal, mensual, trimestral, semestral o anual.
   - **Manejo de Superávit o Déficit**:
     - El presupuesto se reinicia automáticamente en cada periodo.
     - El usuario puede decidir si el saldo del periodo anterior (superávit o déficit) se transfiere al siguiente periodo o se descarta.
     - Ejemplo: Si el presupuesto es de 1000 pesos quincenales y solo se gastan 800 pesos, se puede optar por trasladar los 200 pesos restantes al siguiente periodo o descartarlos.

13. **Compatibilidad Móvil**: La aplicación debe ser fácil de usar desde dispositivos móviles, asegurando una buena experiencia de usuario. Este alcance se considera opcional en la fase inicial backend-first, pero se pueden contemplar:

   - **App Nativa**: Aplicación instalada directamente en el dispositivo, ofreciendo mejor rendimiento y acceso a funcionalidades específicas del hardware.
   - **Portal Web Móvil**: Un sitio web optimizado para dispositivos móviles, accesible desde cualquier navegador sin necesidad de instalación.

   Dependiendo de las necesidades del usuario, una versión web móvil puede ser suficiente, pero una app nativa puede proporcionar una experiencia más fluida y capacidades avanzadas.

14. **Borrado y Eliminación de Información**: El usuario debe ser capaz de borrar toda la información almacenada. Para ello, se requiere un código de autorización especial, el cual se proporciona tras una verificación de seguridad para evitar borrados accidentales. Este código se obtiene a través de un correo electrónico registrado o mediante autenticación de dos factores, garantizando la legitimidad de la solicitud.

15. **Exportación de Datos**: El usuario debe ser capaz de exportar toda su información en diferentes formatos, como Excel o JSON.

16. **Reportes Financieros**: El usuario tiene acceso a una lista de reportes que le ayudarán a entender sus finanzas y su capacidad de pago. Algunos de los reportes disponibles incluyen:

   - **Reporte de Gastos por Etiquetas**: Dentro de un periodo específico (mensual, quincenal, semanal, semestral, anual, o personalizado con fecha de inicio y fin). Incluirá gráficos para facilitar la visualización.
   - **Reporte de Presupuestos**: Seguimiento de presupuestos por etiqueta dentro de un periodo específico.
