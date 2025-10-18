import java.util.*;

// --- MODIFICACIÓN 1: Crear una clase interna para el Token ---
/**
 * Pequeña clase interna para almacenar el tipo de token (ej. "d", "+")
 * y su posición (índice) en la cadena de entrada original.
 */
class Token {
    final String type;
    final int position;

    Token(String type, int position) {
        this.type = type;
        this.position = position;
    }

    @Override
    public String toString() {
        return type; // Esto permite que la impresión de la "Entrada" siga funcionando
    }
}

public class LL1Parser {

    // --- (El resto de las variables de la clase no cambian) ---
    private Map<String, Map<String, String>> table;
    private Set<String> terminals;
    private Set<String> nonTerminals;
    private final String START_SYMBOL = "Expr";
    private final String EPSILON = "e";
    private final String EOF = "$";

    public LL1Parser() {
        // (El constructor y initializeTable() no cambian en absoluto)
        terminals = new HashSet<>(Arrays.asList("+", "-", "*", "/", "(", ")", ".", "d", EOF));
        nonTerminals = new HashSet<>(Arrays.asList(
            "Expr", "Expr'", "Term", "Term'", "Factor", 
            "Num", "Num_tail", "Digits", "Digits'"
        ));
        initializeTable();
    }

    private void initializeTable() {
        // (Esta función es idéntica a la anterior)
        table = new HashMap<>();
        Map<String, String> exprRules = new HashMap<>();
        exprRules.put("(", "Term Expr'");
        exprRules.put("-", "Term Expr'");
        exprRules.put(".", "Term Expr'");
        exprRules.put("d", "Term Expr'");
        table.put("Expr", exprRules);
        Map<String, String> exprPRules = new HashMap<>();
        exprPRules.put("+", "+ Term Expr'"); 
        exprPRules.put("-", "- Term Expr'"); 
        exprPRules.put(")", EPSILON);         
        exprPRules.put(EOF, EPSILON);         
        table.put("Expr'", exprPRules);
        Map<String, String> termRules = new HashMap<>();
        termRules.put("(", "Factor Term'");
        termRules.put("-", "Factor Term'");
        termRules.put(".", "Factor Term'");
        termRules.put("d", "Factor Term'");
        table.put("Term", termRules);
        Map<String, String> termPRules = new HashMap<>();
        termPRules.put("+", EPSILON);         
        termPRules.put("-", EPSILON);         
        termPRules.put("*", "* Factor Term'");
        termPRules.put("/", "/ Factor Term'");
        termPRules.put(")", EPSILON);         
        termPRules.put(EOF, EPSILON);         
        table.put("Term'", termPRules);
        Map<String, String> factorRules = new HashMap<>();
        factorRules.put("(", "( Expr )");    
        factorRules.put("-", "- Factor");    
        factorRules.put(".", "Num");         
        factorRules.put("d", "Num");         
        table.put("Factor", factorRules);
        Map<String, String> numRules = new HashMap<>();
        numRules.put(".", ". Digits");             
        numRules.put("d", "d Digits' Num_tail"); 
        table.put("Num", numRules);
        Map<String, String> numTailRules = new HashMap<>();
        numTailRules.put(".", ". Digits");     
        numTailRules.put("+", EPSILON);      
        numTailRules.put("-", EPSILON);      
        numTailRules.put("*", EPSILON);      
        numTailRules.put("/", EPSILON);      
        numTailRules.put(")", EPSILON);      
        numTailRules.put(EOF, EPSILON);      
        table.put("Num_tail", numTailRules);
        Map<String, String> digitsRules = new HashMap<>();
        digitsRules.put("d", "d Digits'");     
        table.put("Digits", digitsRules);
        Map<String, String> digitsPRules = new HashMap<>();
        digitsPRules.put("d", "d Digits'");    
        digitsPRules.put("+", EPSILON);      
        digitsPRules.put("-", EPSILON);      
        digitsPRules.put("*", EPSILON);      
        digitsPRules.put("/", EPSILON);      
        digitsPRules.put(")", EPSILON);      
        digitsPRules.put(".", EPSILON);      
        digitsPRules.put(EOF, EPSILON);      
        table.put("Digits'", digitsPRules);
    }

    /**
     * --- MODIFICACIÓN 2: El Lexer ahora devuelve List<Token> ---
     * Convierte la cadena de entrada en una lista de Tokens (tipo + posición).
     */
    private List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>(); // Ahora es una lista de Tokens
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                continue; // Ignorar espacios en blanco
            }
            if (Character.isDigit(c)) {
                tokens.add(new Token("d", i)); // Guardamos el tipo "d" y la posición i
            } else if (c == '+') {
                tokens.add(new Token("+", i));
            } else if (c == '-') {
                tokens.add(new Token("-", i));
            } else if (c == '*') {
                tokens.add(new Token("*", i));
            } else if (c == '/') {
                tokens.add(new Token("/", i));
            } else if (c == '(') {
                tokens.add(new Token("(", i));
            } else if (c == ')') {
                tokens.add(new Token(")", i));
            } else if (c == '.') {
                tokens.add(new Token(".", i));
            } else {
                // Error Léxico: Ya reporta la posición
                System.out.println("Error Léxico: Caracter inválido '" + c + "' en la posición " + i);
                tokens.clear();
                tokens.add(new Token("ERROR", i)); // Token de error con posición
                return tokens;
            }
        }
        tokens.add(new Token(EOF, input.length())); // Añadir el token de fin de cadena
        return tokens;
    }

    /**
     * --- MODIFICACIÓN 3: El Parser ahora usa List<Token> y reporta errores ---
     */
    public List<Token> parse(String input) {
        System.out.println("Analizando cadena: \"" + input + "\"\n");
        
        // 1. Obtener la lista de tokens
        List<Token> tokens = tokenize(input);
        if (tokens.get(0).type.equals("ERROR")) { // Comprobar si el lexer falló
            System.out.println("Resultado: RECHAZADA (Error léxico)");
            return null;
        }

        // 2. Inicializar la Pila (sin cambios)
        Stack<String> stack = new Stack<>();
        stack.push(EOF);
        stack.push(START_SYMBOL);

        // 3. Inicializar puntero de entrada (sin cambios)
        int inputPointer = 0;
        boolean accepted = true;

        // Imprimir encabezado (sin cambios)
        System.out.printf("%-45s | %-25s | %s\n", "PILA (tope a la derecha)", "ENTRADA (inicio a la izquierda)", "ACCION");
        System.out.println(new String(new char[100]).replace("\0", "-"));

        // 4. Iniciar el ciclo del parser
        while (!stack.peek().equals(EOF)) {
            String stackTop = stack.peek();
            
            // --- MODIFICACIÓN: Obtener el token y su tipo ---
            Token currentTokenObj = tokens.get(inputPointer);
            String currentToken = currentTokenObj.type;
            int currentPosition = currentTokenObj.position; // ¡Aquí está la posición!

            // Imprimir el estado actual
            System.out.printf("%-45s | %-25s | ", formatStack(stack), formatInput(tokens, inputPointer));

            // --- Escenario 1: El tope de la pila es un TERMINAL ---
            if (terminals.contains(stackTop)) {
                if (stackTop.equals(currentToken)) {
                    // ¡Coincidencia! (Match)
                    System.out.println("Match: " + currentToken);
                    stack.pop();
                    inputPointer++;
                } else {
                    // --- MODIFICACIÓN: Error de Mismatch (Incoherencia) ---
                    System.out.println("Error Sintáctico: Se esperaba '" + stackTop + "' pero se encontró '" + 
                                       currentToken + "' en la posición " + currentPosition);
                    accepted = false;
                    break;
                }
            } 
            // --- Escenario 2: El tope de la pila es un NO-TERMINAL ---
            else if (nonTerminals.contains(stackTop)) {
                String rule = null;
                if (table.containsKey(stackTop) && table.get(stackTop).containsKey(currentToken)) {
                    rule = table.get(stackTop).get(currentToken);
                }

                if (rule == null) {
                    // --- MODIFICACIÓN: Error de Token Inesperado (Celda vacía) ---
                    System.out.println("Error Sintáctico: Token inesperado '" + currentToken + 
                                       "' en la posición " + currentPosition + 
                                       " mientras se procesaba '" + stackTop + "'.");
                    accepted = false;
                    break;
                }

                // Aplicar la regla (sin cambios)
                System.out.println("Usar regla: " + stackTop + " -> " + rule);
                stack.pop();
                if (!rule.equals(EPSILON)) {
                    String[] symbols = rule.split(" ");
                    for (int i = symbols.length - 1; i >= 0; i--) {
                        stack.push(symbols[i]);
                    }
                }
            } else {
                // Error desconocido (sin cambios)
                System.out.println("Error: Símbolo desconocido en la pila: " + stackTop);
                accepted = false;
                break;
            }
        } // Fin del while

        System.out.println(new String(new char[100]).replace("\0", "-"));

        // --- 5. Resultado Final ---
        // --- MODIFICACIÓN: Comprobar el *tipo* del token actual ---
        if (accepted && tokens.get(inputPointer).type.equals(EOF) && stack.peek().equals(EOF)) {
            System.out.printf("%-45s | %-25s | %s\n", formatStack(stack), formatInput(tokens, inputPointer), "¡Éxito!");
            System.out.println("\nResultado: ACEPTADA ✅");
			return tokens;
        } else {
            System.out.println("\nResultado: RECHAZADA ❌");
			return null;
        }
    }

    // --- Métodos auxiliares ---
    
    // formatStack no cambia
    private String formatStack(Stack<String> stack) {
        return stack.toString();
    }

    /**
     * --- MODIFICACIÓN 4: formatInput ahora usa List<Token> ---
     */
    private String formatInput(List<Token> tokens, int pointer) {
        StringBuilder sb = new StringBuilder();
        for (int i = pointer; i < tokens.size(); i++) {
            sb.append(tokens.get(i).type).append(" "); // Usamos .type
        }
        return sb.toString();
    }

    /**
     * Método main (sin cambios)
     */
    public static void main(String[] args) {
        LL1Parser parser = new LL1Parser();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("--- Analizador Sintáctico LL(1) para Expresiones ---");
        System.out.println("Gramática usa 'd' para cualquier dígito.");
        System.out.println("Ejemplos válidos: 1.2+3  |  (12*3)-.4  |  -5/(2+1)");
        System.out.println("Escriba una 'palabra' (expresión) o 'salir' para terminar:");

        while(true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("salir")) {
                break;
            }
            if (input.trim().isEmpty()) {
                continue;
            }
            
            parser.parse(input);
        }
        
        scanner.close();
        System.out.println("Adiós.");
    }
}