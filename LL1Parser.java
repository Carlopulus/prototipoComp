import java.util.*;

// --- MODIFICACIÓN 1: 'Token' ahora guarda su 'value' (valor) original ---
/**
 * Almacena el tipo de token (ej. "d", "+"), su valor original (ej. "5", "+")
 * y su posición en la cadena de entrada.
 */
class Token {
    final String type;  // El tipo ("d", "+", "number", "(", etc.)
    final String value; // El lexema o valor ("5", "+", "1.2", "(")
    final int position;

    Token(String type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    @Override
    public String toString() {
        // Al imprimir la "Entrada", ahora veremos el valor real, no solo el tipo
        return value; 
    }
}

public class LL1Parser {

    // (Variables de la clase LL1Parser)
    private Map<String, Map<String, String>> table;
    private Set<String> terminals;
    private Set<String> nonTerminals;
    private final String START_SYMBOL = "Expr";
    private final String EPSILON = "e";
    private final String EOF = "$";

    public LL1Parser() {
        // 'd' sigue siendo el *tipo* de token para un dígito
        terminals = new HashSet<String>(Arrays.asList("+", "-", "*", "/", "(", ")", ".", "d", EOF));
        nonTerminals = new HashSet<String>(Arrays.asList(
            "Expr", "Expr'", "Term", "Term'", "Factor", 
            "Num", "Num_tail", "Digits", "Digits'"
        ));
        
        initializeTable(); // La tabla sigue siendo la misma
    }

    private void initializeTable() {
        // (Esta función es idéntica a la anterior, no la repetiré)
        
        // --- CAMBIOS AQUÍ: Especificación completa de tipos genéricos ---
        table = new HashMap<String, Map<String, String>>();
        Map<String, String> exprRules = new HashMap<String, String>();
        exprRules.put("(", "Term Expr'");
        exprRules.put("-", "Term Expr'");
        exprRules.put(".", "Term Expr'");
        exprRules.put("d", "Term Expr'");
        table.put("Expr", exprRules);
        Map<String, String> exprPRules = new HashMap<String, String>();
        exprPRules.put("+", "+ Term Expr'"); 
        exprPRules.put("-", "- Term Expr'"); 
        exprPRules.put(")", EPSILON);         
        exprPRules.put(EOF, EPSILON);         
        table.put("Expr'", exprPRules);
        Map<String, String> termRules = new HashMap<String, String>();
        termRules.put("(", "Factor Term'");
        termRules.put("-", "Factor Term'");
        termRules.put(".", "Factor Term'");
        termRules.put("d", "Factor Term'");
        table.put("Term", termRules);
        Map<String, String> termPRules = new HashMap<String, String>();
        termPRules.put("+", EPSILON);         
        termPRules.put("-", EPSILON);         
        termPRules.put("*", "* Factor Term'");
        termPRules.put("/", "/ Factor Term'");
        termPRules.put(")", EPSILON);         
        termPRules.put(EOF, EPSILON);         
        table.put("Term'", termPRules);
        Map<String, String> factorRules = new HashMap<String, String>();
        factorRules.put("(", "( Expr )");    
        factorRules.put("-", "- Factor");    
        factorRules.put(".", "Num");         
        factorRules.put("d", "Num");         
        table.put("Factor", factorRules);
        Map<String, String> numRules = new HashMap<String, String>();
        numRules.put(".", ". Digits");             
        numRules.put("d", "d Digits' Num_tail"); 
        table.put("Num", numRules);
        Map<String, String> numTailRules = new HashMap<String, String>();
        numTailRules.put(".", ". Digits");     
        numTailRules.put("+", EPSILON);      
        numTailRules.put("-", EPSILON);      
        numTailRules.put("*", EPSILON);      
        numTailRules.put("/", EPSILON);      
        numTailRules.put(")", EPSILON);      
        numTailRules.put(EOF, EPSILON);      
        table.put("Num_tail", numTailRules);
        Map<String, String> digitsRules = new HashMap<String, String>();
        digitsRules.put("d", "d Digits'");     
        table.put("Digits", digitsRules);
        Map<String, String> digitsPRules = new HashMap<String, String>();
        digitsPRules.put("d", "d Digits'");    
        digitsPRules.put("+", EPSILON);      
        digitsPRules.put("-", EPSILON);      
        digitsPRules.put("*", EPSILON);      
        digitsPRules.put("/", EPSILON);      
        digitsPRules.put(")", EPSILON);      
        digitsPRules.put(".", EPSILON);      
        digitsPRules.put(EOF, EPSILON);      
        table.put("Digits'", digitsPRules);
        // --- FIN DE LOS CAMBIOS EN initializeTable ---
    }

    /**
     * --- MODIFICACIÓN 2: 'tokenize' ahora guarda el valor del caracter ---
     */
    private List<Token> tokenize(String input) {
        // --- CAMBIO AQUÍ ---
        List<Token> tokens = new ArrayList<Token>();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String val = String.valueOf(c);

            if (Character.isWhitespace(c)) continue;
            
            // El *tipo* es "d", pero el *valor* es el dígito real ("0", "1", "5", etc.)
            if (Character.isDigit(c)) {
                tokens.add(new Token("d", val, i));
            } else if (c == '+') {
                tokens.add(new Token("+", val, i));
            } else if (c == '-') {
                tokens.add(new Token("-", val, i));
            } else if (c == '*') {
                tokens.add(new Token("*", val, i));
            } else if (c == '/') {
                tokens.add(new Token("/", val, i));
            } else if (c == '(') {
                tokens.add(new Token("(", val, i));
            } else if (c == ')') {
                tokens.add(new Token(")", val, i));
            } else if (c == '.') {
                tokens.add(new Token(".", val, i));
            } else {
                System.out.println("Error Léxico: Caracter inválido '" + c + "' en la posición " + i);
                tokens.clear();
                tokens.add(new Token("ERROR", val, i));
                return tokens;
            }
        }
        tokens.add(new Token(EOF, EOF, input.length()));
        return tokens;
    }

    /**
     * --- MODIFICACIÓN 3: 'parse' ahora devuelve List<Token> o null ---
     */
    public List<Token> parse(String input) {
        System.out.println("Analizando cadena: \"" + input + "\"\n");
        
        List<Token> tokens = tokenize(input);
        if (tokens.get(0).type.equals("ERROR")) {
            System.out.println("Resultado: RECHAZADA (Error léxico)");
            return null; // Devuelve null si falla
        }

        // --- CAMBIO AQUÍ ---
        Stack<String> stack = new Stack<String>();
        stack.push(EOF);
        stack.push(START_SYMBOL);
        int inputPointer = 0;

        System.out.printf("%-45s | %-25s | %s\n", "PILA (tope a la derecha)", "ENTRADA (inicio a la izquierda)", "ACCION");
        System.out.println(new String(new char[100]).replace("\0", "-"));

        while (!stack.peek().equals(EOF)) {
            String stackTop = stack.peek();
            Token currentTokenObj = tokens.get(inputPointer);
            String currentToken = currentTokenObj.type;
            int currentPosition = currentTokenObj.position;

            System.out.printf("%-45s | %-25s | ", formatStack(stack), formatInput(tokens, inputPointer));

            if (terminals.contains(stackTop)) {
                if (stackTop.equals(currentToken)) {
                    System.out.println("Match: " + currentTokenObj.value); // Imprime el valor
                    stack.pop();
                    inputPointer++;
                } else {
                    System.out.println("Error Sintáctico: Se esperaba '" + stackTop + "' pero se encontró '" + 
                                       currentTokenObj.value + "' en la posición " + currentPosition);
                    System.out.println(new String(new char[100]).replace("\0", "-"));
                    System.out.println("\nResultado: RECHAZADA ❌");
                    return null; // Devuelve null si falla
                }
            } 
            else if (nonTerminals.contains(stackTop)) {
                String rule = null;
                if (table.containsKey(stackTop) && table.get(stackTop).containsKey(currentToken)) {
                    rule = table.get(stackTop).get(currentToken);
                }

                if (rule == null) {
                    System.out.println("Error Sintáctico: Token inesperado '" + currentTokenObj.value + 
                                       "' en la posición " + currentPosition + 
                                       " mientras se procesaba '" + stackTop + "'.");
                    System.out.println(new String(new char[100]).replace("\0", "-"));
                    System.out.println("\nResultado: RECHAZADA ❌");
                    return null; // Devuelve null si falla
                }

                System.out.println("Usar regla: " + stackTop + " -> " + rule);
                stack.pop();
                if (!rule.equals(EPSILON)) {
                    String[] symbols = rule.split(" ");
                    for (int i = symbols.length - 1; i >= 0; i--) {
                        stack.push(symbols[i]);
                    }
                }
            } else {
                System.out.println("Error: Símbolo desconocido en la pila: " + stackTop);
                System.out.println(new String(new char[100]).replace("\0", "-"));
                System.out.println("\nResultado: RECHAZADA ❌");
                return null; // Devuelve null si falla
            }
        } // Fin del while

        System.out.println(new String(new char[100]).replace("\0", "-"));
        
        if (tokens.get(inputPointer).type.equals(EOF) && stack.peek().equals(EOF)) {
            System.out.printf("%-45s | %-25s | %s\n", formatStack(stack), formatInput(tokens, inputPointer), "¡Éxito!");
            System.out.println("\nResultado: ACEPTADA ✅");
            return tokens; // ¡Éxito! Devuelve la lista de tokens
        } else {
            System.out.println("\nResultado: RECHAZADA ❌ (final inesperado)");
            return null; // Devuelve null si falla
        }
    }

    // --- Métodos auxiliares de impresión (modificados para List<Token>) ---
    private String formatStack(Stack<String> stack) {
        return stack.toString();
    }

    private String formatInput(List<Token> tokens, int pointer) {
        StringBuilder sb = new StringBuilder();
        for (int i = pointer; i < tokens.size(); i++) {
            sb.append(tokens.get(i).value).append(" "); // Usamos .value
        }
        return sb.toString();
    }

/*
     * --- PUNTO 2: CREADOR DEL ÁRBOL (AST) ---
     * Esta clase interna toma la lista de tokens y la convierte en un árbol.
*/

    public static class TreeBuilder {
        
        // --- Clases para los Nodos del Árbol ---
        
        // La interfaz ahora define dos métodos: uno público y uno auxiliar
        public static interface ExprNode {
            /** Inicia el proceso de impresión del árbol. */
            String prettyPrint();
            
            /** * Método auxiliar recursivo para construir la cadena del árbol.
             * @param buffer El StringBuilder para construir la salida.
             * @param prefix El prefijo para la línea actual (ej. "└── ").
             * @param childrenPrefix El prefijo para las líneas de los hijos (ej. "    ").
             */
            void prettyPrint(StringBuilder buffer, String prefix, String childrenPrefix);
        }

        static class NumberNode implements ExprNode {
            String value;
            NumberNode(String value) { this.value = value; }
            
            public String prettyPrint() {
                StringBuilder buffer = new StringBuilder();
                prettyPrint(buffer, "", "");
                return buffer.toString();
            }

            public void prettyPrint(StringBuilder buffer, String prefix, String childrenPrefix) {
                buffer.append(prefix);
                buffer.append(value);
                buffer.append("\n");
            }
        }

        static class UnaryOpNode implements ExprNode {
            String operator;
            ExprNode child;
            UnaryOpNode(String op, ExprNode child) { this.operator = op; this.child = child; }
            
            public String prettyPrint() {
                StringBuilder buffer = new StringBuilder();
                prettyPrint(buffer, "", "");
                return buffer.toString();
            }

            public void prettyPrint(StringBuilder buffer, String prefix, String childrenPrefix) {
                buffer.append(prefix);
                buffer.append(operator);
                buffer.append("\n");
                // El hijo es el "último" (y único)
                child.prettyPrint(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }

        static class BinaryOpNode implements ExprNode {
            String operator;
            ExprNode left, right;
            BinaryOpNode(ExprNode left, String op, ExprNode right) {
                this.left = left; this.operator = op; this.right = right;
            }

            public String prettyPrint() {
                StringBuilder buffer = new StringBuilder();
                prettyPrint(buffer, "", "");
                return buffer.toString();
            }

            public void prettyPrint(StringBuilder buffer, String prefix, String childrenPrefix) {
                buffer.append(prefix);
                buffer.append(operator);
                buffer.append("\n");
                // El hijo izquierdo (left) NO es el último, usa "tee"
                left.prettyPrint(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
                // El hijo derecho (right) SÍ es el último, usa "corner"
                right.prettyPrint(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
        
        // --- Lógica del Parser de Árbol (Descenso Recursivo) ---
        // (build(), expr(), expr_prime(), term(), term_prime(), factor(), ...)
        
        
        private List<Token> tokens;
        private int position;

        public TreeBuilder(List<Token> tokens) {
            this.tokens = tokens;
            this.position = 0;
        }

        private Token peek() { return tokens.get(position); }
        private Token consume() { return tokens.get(position++); }
        private Token match(String type) {
            if (peek().type.equals(type)) {
                return consume();
            }
            throw new RuntimeException("Error de construcción de árbol: Se esperaba " + type + " pero se encontró " + peek().type);
        }

         public ExprNode build() {
            try {
                return expr();
            } catch (Exception e) {
                System.out.println("Error durante la construcción del árbol: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        private ExprNode expr() {
            ExprNode leftNode = term();
            return expr_prime(leftNode);
        }

        private ExprNode expr_prime(ExprNode leftNode) {
            if (peek().type.equals("+")) {
                Token op = consume();
                ExprNode rightNode = term();
                return expr_prime(new BinaryOpNode(leftNode, op.value, rightNode));
            }
            if (peek().type.equals("-")) {
                Token op = consume();
                ExprNode rightNode = term();
                return expr_prime(new BinaryOpNode(leftNode, op.value, rightNode));
            }
            return leftNode;
        }

        private ExprNode term() {
            ExprNode leftNode = factor();
            return term_prime(leftNode);
        }

        private ExprNode term_prime(ExprNode leftNode) {
            if (peek().type.equals("*")) {
                Token op = consume();
                ExprNode rightNode = factor();
                return term_prime(new BinaryOpNode(leftNode, op.value, rightNode));
            }
            if (peek().type.equals("/")) {
                Token op = consume();
                ExprNode rightNode = factor();
                return term_prime(new BinaryOpNode(leftNode, op.value, rightNode));
            }
            return leftNode;
        }

        private ExprNode factor() {
            if (peek().type.equals("(")) {
                consume();
                ExprNode node = expr();
                match(")");
                return node;
            }
            if (peek().type.equals("-")) {
                Token op = consume();
                ExprNode child = factor();
                return new UnaryOpNode(op.value, child);
            }
            return num();
        }
        
        private ExprNode num() {
            String numStr = "";
            if (peek().type.equals(".")) {
                numStr += consume().value;
                numStr += digits();
            } else {
                numStr += digits();
                numStr += num_tail();
            }
            return new NumberNode(numStr);
        }

        private String num_tail() {
            if (peek().type.equals(".")) {
                String val = consume().value;
                val += digits();
                return val;
            }
            return "";
        }
        
        private  String digits() {
            Token d = match("d");
            return d.value + digits_prime();
        }

        private String digits_prime() {
            if (peek().type.equals("d")) {
                Token d = consume();
                return d.value + digits_prime();
            }
            return "";
        }

    } // --- Fin de la clase TreeBuilder ---


    /**
     * Método main modificado para construir el árbol si el análisis es exitoso.
     */
    public static void main(String[] args) {
        LL1Parser parser = new LL1Parser();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("--- Analizador Sintáctico LL(1) y Constructor de Árbol ---");
		System.out.println("Ejemplos válidos: 1.2+3  |  (12*3)-.4  |  -5/(2+1)");
        System.out.println("Escriba una 'palabra' (expresión) o 'salir' para terminar:");

        while(true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("salir")) break;
            if (input.trim().isEmpty()) continue;
            
            // 1. Validar y obtener tokens
            List<Token> tokens = parser.parse(input);
            
            // 2. Si la validación fue exitosa, construir el árbol
            if (tokens != null) {
                System.out.println("\nConstruyendo Árbol de Operaciones (AST)...");
                // Recuerda que TreeBuilder y ExprNode son estáticas internas
                TreeBuilder builder = new TreeBuilder(tokens);
                TreeBuilder.ExprNode tree = builder.build();
                
                if (tree != null) {
                    System.out.println("--- Árbol Generado ---");
                    // --- MODIFICACIÓN: Llamada al nuevo método de impresión ---
                    System.out.print(tree.prettyPrint());
                    System.out.println("----------------------");
                } else {
                    System.out.println("Fallo en la construcción del árbol.");
                }
            }
        }
        
        scanner.close();
        System.out.println("Adiós.");
    }
}