import java.util.HashMap;

class UnimplementedAbility extends Ability{
  public String realUse(Player player){
    String resultString = "NOT IMPLEMENTED! OH NOOOOOOOOOOOOOO!";
    return resultString;
  }

  UnimplementedAbility(){
    this.name = "Unimplemented Ability";
    this.energyRequired = new HashMap<EnergyCard, Integer>();
  }
  
  public String getDescription(){
	  return null;
  }
}
