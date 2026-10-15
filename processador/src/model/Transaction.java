package model;

public class Transaction {
    private int id;
    private String cliente;
    private String data;
    private double valor;
    private String categoria;
    private String cidade;
    private String estado;
    private String tipo;

    public Transaction() {}

    public Transaction(int id, String cliente, String data, double valor, String categoria, String cidade, String estado, String tipo) {
        this.id = id;
        this.cliente = cliente;
        this.data = data;
        this.valor = valor;
        this.categoria = categoria;
        this.cidade = cidade;
        this.estado = estado;
        this.tipo = tipo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", cliente='" + cliente + '\'' +
                ", data='" + data + '\'' +
                ", valor=" + valor +
                ", categoria='" + categoria + '\'' +
                ", cidade='" + cidade + '\'' +
                ", estado='" + estado + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}
