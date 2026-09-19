package com.example.Calculator.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.Calculator.Service.CalculatorService;

@Controller
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
		this.calculatorService = calculatorService;
	}


    @GetMapping("/")
    public String showPage() {

        return "calculator";
    }


    @PostMapping("/calculate")
    public String calculate(
            @RequestParam String expression,
            @RequestParam(defaultValue = "DEG") String angleMode,
            Model model) {

        try {

            double result =
                    calculatorService.calculate(
                            expression,
                            angleMode
                    );


            model.addAttribute(
                    "expression",
                    expression
            );

            model.addAttribute(
                    "result",
                    result
            );

            model.addAttribute(
                    "angleMode",
                    angleMode
            );


            return "result";

        } catch (ArithmeticException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "calculator";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "calculator";
        }
    }
}
