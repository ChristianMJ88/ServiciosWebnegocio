package fluora.postman;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ApiAisladaApplicationTest {
 @Test void rechazaPlantillaYBaseHabitualAntesDeSpring(){for(String db:new String[]{null,"agenda_db","fluora_test_base","fluora_test_011_abc;DROP"})assertThatThrownBy(()->ApiAisladaApplication.comprobarBase(db)).isInstanceOf(IllegalStateException.class);}
 @Test void admiteSoloCopiaDeLaSuite(){assertThatCode(()->ApiAisladaApplication.comprobarBase("fluora_test_011_abc123")).doesNotThrowAnyException();}
}
