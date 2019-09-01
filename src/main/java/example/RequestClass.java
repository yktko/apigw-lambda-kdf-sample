package example;
public class RequestClass {
    String firstName;
    String lastName;
    String values;
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getValues() {
        return values;
    }
    public void setValues(String values) {
        this.values = values;
    }
    public RequestClass(String firstName, String lastName, String values) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.values = values;
    }
    public RequestClass() {
    }
}
